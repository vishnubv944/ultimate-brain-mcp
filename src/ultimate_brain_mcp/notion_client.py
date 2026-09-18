"""Async httpx wrapper for the Notion API (version 2025-09-03).

This module abstracts away three Notion API limits so callers can submit
arbitrarily large/deep documents:

1. Children-array cap: any individual `children` array is limited to 100
   elements, including the top-level array on `POST /pages` and
   `PATCH /blocks/{id}/children`.
2. Nesting cap: a single request may contain at most 2 levels of block
   nesting. Deeper subtrees are deferred and re-appended after the parent
   block id is known.
3. Rate limit: Notion documents an average of 3 requests/sec. A
   per-client token-spaced limiter serialises requests, and 429 responses
   trigger Retry-After sleeps with exponential backoff.

See https://developers.notion.com/reference/request-limits.
"""

from __future__ import annotations

import asyncio
import copy
from typing import Any

import httpx

NOTION_BASE = "https://api.notion.com/v1"
NOTION_VERSION = "2025-09-03"
# The page-markdown endpoints (GET/PATCH /pages/{id}/markdown) require a newer
# API version than the rest of the surface. We pin it per-call rather than
# globally so all other endpoints stay on the stable 2025-09-03 contract.
MARKDOWN_NOTION_VERSION = "2026-03-11"

# Notion-documented limits (https://developers.notion.com/reference/request-limits)
MAX_CHILDREN_PER_ARRAY = 100
MAX_NESTING_DEPTH = 2
DEFAULT_RATE_PER_SEC = 3.0
RETRY_STATUSES = {429, 502, 503, 504}
MAX_RETRIES = 5
MAX_BACKOFF_SECONDS = 30.0

# Read-side safety cap. Notion has no documented limit on blocks per page,
# but unbounded recursion on a corrupted structure would be costly. ~5000
# blocks comfortably exceeds typical document size.
MAX_READ_PAGES = 50  # 50 pages * 100 blocks/page = 5000 blocks


class NotionAPIError(Exception):
    """Raised when the Notion API returns a non-2xx response."""

    def __init__(self, status: int, code: str, message: str) -> None:
        self.status = status
        self.code = code
        super().__init__(message)


class PartialWriteError(NotionAPIError):
    """Raised when a chunked write fails after some blocks were already written.

    Carries the underlying error plus counts so the caller can report or
    resume from where it stopped.
    """

    def __init__(
        self,
        original: NotionAPIError,
        *,
        written: int,
        remaining: int,
        page_id: str = "",
    ) -> None:
        super().__init__(original.status, original.code, str(original))
        self.written = written
        self.remaining = remaining
        self.page_id = page_id


# ---------------------------------------------------------------------------
# Block helpers
# ---------------------------------------------------------------------------


def _block_data(block: dict) -> dict:
    """Return the inner data dict for a block (where rich_text / children live)."""
    btype = block.get("type", "")
    return block.get(btype, {})


def _split_for_depth(
    blocks: list[dict], *, max_depth: int = MAX_NESTING_DEPTH
) -> tuple[list[dict], list[tuple[list[int], list[dict]]]]:
    """Trim a block tree so it fits within Notion's nesting cap.

    Walks *blocks* (depth 1 = top-level) and, for any block at exactly
    *max_depth* that carries children, removes those children and records
    them in the returned `deferred` list along with the index path needed
    to locate the parent block in the trimmed tree.

    Returns ``(top_blocks, deferred)`` where:

    - ``top_blocks`` is a deep copy of *blocks* with depth ≤ *max_depth*,
      safe to send in a single Notion request.
    - ``deferred`` is a list of ``(path, children)`` tuples. Each path is
      a list of integer indices: ``path[0]`` indexes into ``top_blocks``,
      ``path[1]`` (if present) indexes into that block's children array,
      and so on.

    Does not mutate *blocks*.
    """
    top = copy.deepcopy(blocks)
    deferred: list[tuple[list[int], list[dict]]] = []

    def walk(block_list: list[dict], depth: int, path: list[int]) -> None:
        for idx, block in enumerate(block_list):
            data = _block_data(block)
            children = data.get("children")
            if not children:
                continue
            if depth >= max_depth:
                deferred.append((path + [idx], children))
                data.pop("children", None)
            else:
                walk(children, depth + 1, path + [idx])

    walk(top, depth=1, path=[])
    return top, deferred


# ---------------------------------------------------------------------------
# Rate limiter
# ---------------------------------------------------------------------------


class _RateLimiter:
    """Serialises async callers to at most *rate* requests per second.

    Implemented as a leaky-bucket: each `acquire()` pushes a shared
    "next-allowed" timestamp forward by 1/rate seconds. Concurrent callers
    queue on the lock and sleep their share before returning.
    """

    def __init__(self, rate: float) -> None:
        self._interval = 1.0 / rate if rate > 0 else 0.0
        self._next_ok = 0.0
        self._lock = asyncio.Lock()

    async def acquire(self) -> None:
        if self._interval <= 0:
            return
        async with self._lock:
            loop = asyncio.get_event_loop()
            now = loop.time()
            wait = self._next_ok - now
            if wait > 0:
                await asyncio.sleep(wait)
                now = loop.time()
            self._next_ok = max(now, self._next_ok) + self._interval


class NotionClient:
    """Lightweight async Notion API client using httpx.

    Handles the API's children-array cap (100), nesting cap (2 levels),
    and rate limit (~3 req/sec) transparently.
    """

    def __init__(self, secret: str, *, rate_per_sec: float = DEFAULT_RATE_PER_SEC) -> None:
        self._client = httpx.AsyncClient(
            base_url=NOTION_BASE,
            headers={
                "Authorization": f"Bearer {secret}",
                "Notion-Version": NOTION_VERSION,
                "Content-Type": "application/json",
            },
            timeout=30.0,
        )
        self._limiter = _RateLimiter(rate_per_sec)

    async def close(self) -> None:
        await self._client.aclose()

    # ------------------------------------------------------------------
    # Internal: rate-limited request with retry on 429/5xx
    # ------------------------------------------------------------------

    async def _request(
        self,
        method: str,
        url: str,
        *,
        notion_version: str | None = None,
        **kwargs: Any,
    ) -> httpx.Response:
        # Per-call API version override. The client default (2025-09-03) is set
        # on the AsyncClient; passing notion_version layers an endpoint-specific
        # version (e.g. the page-markdown endpoints' 2026-03-11) onto this call
        # only, mirroring the official server's per-operation version pinning.
        if notion_version is not None:
            headers = dict(kwargs.pop("headers", {}) or {})
            headers["Notion-Version"] = notion_version
            kwargs["headers"] = headers
        backoff = 1.0
        last_error: NotionAPIError | None = None
        for attempt in range(MAX_RETRIES + 1):
            await self._limiter.acquire()
            resp = await self._client.request(method, url, **kwargs)
            if resp.is_success:
                return resp
            if resp.status_code in RETRY_STATUSES and attempt < MAX_RETRIES:
                retry_after = self._retry_after_seconds(resp, fallback=backoff)
                await asyncio.sleep(min(retry_after, MAX_BACKOFF_SECONDS))
                backoff = min(backoff * 2, MAX_BACKOFF_SECONDS)
                last_error = self._build_error(resp)
                continue
            self._raise_for_status(resp)
        # Exhausted retries
        if last_error is not None:
            raise last_error
        raise NotionAPIError(0, "unknown", "request failed without response")

    @staticmethod
    def _retry_after_seconds(resp: httpx.Response, *, fallback: float) -> float:
        header = resp.headers.get("Retry-After")
        if not header:
            return fallback
        try:
            return float(header)
        except ValueError:
            return fallback

    def _raise_for_status(self, resp: httpx.Response) -> None:
        if resp.is_success:
            return
        raise self._build_error(resp)

    @staticmethod
    def _build_error(resp: httpx.Response) -> NotionAPIError:
        body: dict = {}
        if resp.headers.get("content-type", "").startswith("application/json"):
            try:
                body = resp.json()
            except ValueError:
                body = {}
        code = body.get("code", "unknown")
        message = body.get("message", resp.text)
        return NotionAPIError(resp.status_code, code, message)

    # ------------------------------------------------------------------
    # Query data source (replaces /databases/{id}/query in 2025-09-03)
    # ------------------------------------------------------------------

    async def query_data_source(
        self,
        ds_id: str,
        *,
        filter: dict | None = None,
        sorts: list[dict] | None = None,
        page_size: int = 100,
        start_cursor: str | None = None,
    ) -> dict:
        """POST /v1/data_sources/{ds_id}/query — returns the raw response dict."""
        body: dict = {"page_size": page_size}
        if filter:
            body["filter"] = filter
        if sorts:
            body["sorts"] = sorts
        if start_cursor:
            body["start_cursor"] = start_cursor
        resp = await self._request("POST", f"/data_sources/{ds_id}/query", json=body)
        return resp.json()

    async def query_all(
        self,
        ds_id: str,
        *,
        filter: dict | None = None,
        sorts: list[dict] | None = None,
        max_pages: int = 5,
    ) -> list[dict]:
        """Paginate through all results (up to max_pages pages). Returns flat list of pages."""
        all_results: list[dict] = []
        cursor: str | None = None
        for _ in range(max_pages):
            data = await self.query_data_source(
                ds_id, filter=filter, sorts=sorts, start_cursor=cursor
            )
            all_results.extend(data.get("results", []))
            if not data.get("has_more"):
                break
            cursor = data.get("next_cursor")
        return all_results

    # ------------------------------------------------------------------
    # Page CRUD
    # ------------------------------------------------------------------

    async def create_page(
        self,
        ds_id: str,
        properties: dict,
        *,
        children: list[dict] | None = None,
        template: dict | None = None,
    ) -> dict:
        """POST /v1/pages — create a page in the given data source.

        Accepts arbitrarily large/deep *children*. Internally chunks the
        first 100 top-level blocks into the create call, appends the
        remainder, and re-appends any subtrees deeper than 2 levels.

        *template* applies a data source template, e.g. ``{"type": "default"}``
        or ``{"type": "template_id", "template_id": "..."}`` — see
        :meth:`list_templates`. Per the Notion API, a template and *children*
        are mutually exclusive on create; passing both raises ``ValueError``
        rather than letting Notion reject it with a less clear 400.
        """
        if template and children:
            raise ValueError("template and children are mutually exclusive on page create")

        body: dict = {
            "parent": {"data_source_id": ds_id},
            "properties": properties,
        }
        if template:
            body["template"] = template

        if not children:
            resp = await self._request("POST", "/pages", json=body)
            return resp.json()

        top, deferred = _split_for_depth(children)
        first_batch = top[:MAX_CHILDREN_PER_ARRAY]
        rest = top[MAX_CHILDREN_PER_ARRAY:]

        body["children"] = first_batch
        try:
            resp = await self._request("POST", "/pages", json=body)
        except NotionAPIError as e:
            raise PartialWriteError(e, written=0, remaining=len(top), page_id="") from e

        page = resp.json()
        page_id = page["id"]
        written = len(first_batch)

        if rest:
            try:
                await self.append_blocks(page_id, rest)
            except NotionAPIError as e:
                # Determine how much of rest was written
                already = getattr(e, "written", 0) if isinstance(e, PartialWriteError) else 0
                raise PartialWriteError(
                    e,
                    written=written + already,
                    remaining=len(top) - written - already,
                    page_id=page_id,
                ) from e
            written += len(rest)

        if deferred:
            try:
                top_blocks = await self.get_blocks(page_id)
                await self._flush_deferred(top_blocks, deferred)
            except NotionAPIError as e:
                # Top-level write succeeded; only deep children failed.
                raise PartialWriteError(
                    e,
                    written=written,
                    remaining=sum(len(kids) for _, kids in deferred),
                    page_id=page_id,
                ) from e

        return page

    async def get_page(self, page_id: str) -> dict:
        """GET /v1/pages/{page_id}"""
        resp = await self._request("GET", f"/pages/{page_id}")
        return resp.json()

    async def update_page(self, page_id: str, properties: dict) -> dict:
        """PATCH /v1/pages/{page_id}"""
        resp = await self._request("PATCH", f"/pages/{page_id}", json={"properties": properties})
        return resp.json()

    # ------------------------------------------------------------------
    # Page content as Markdown (server-side conversion, 2026-03-11+)
    # ------------------------------------------------------------------

    async def get_page_markdown(self, page_id: str) -> dict:
        """GET /v1/pages/{page_id}/markdown — page body as enhanced Markdown.

        Returns the raw response dict: ``markdown`` (str), ``truncated`` (bool),
        and ``unknown_block_ids`` (list) for blocks that could not be rendered.
        Notion handles all block types server-side, including tables, toggles,
        and callouts that our text converters do not cover.
        """
        resp = await self._request(
            "GET",
            f"/pages/{page_id}/markdown",
            notion_version=MARKDOWN_NOTION_VERSION,
        )
        return resp.json()

    async def replace_page_markdown(
        self, page_id: str, markdown: str, *, allow_deleting_content: bool = False
    ) -> dict:
        """PATCH /v1/pages/{page_id}/markdown — overwrite the whole page body.

        Notion performs block splitting/nesting server-side, so this replaces
        the chunked append machinery for full-body writes.
        """
        body = {
            "type": "replace_content",
            "replace_content": {
                "new_str": markdown,
                "allow_deleting_content": allow_deleting_content,
            },
        }
        resp = await self._request(
            "PATCH",
            f"/pages/{page_id}/markdown",
            json=body,
            notion_version=MARKDOWN_NOTION_VERSION,
        )
        return resp.json()

    async def update_page_markdown(
        self,
        page_id: str,
        content_updates: list[dict],
        *,
        allow_deleting_content: bool = False,
    ) -> dict:
        """PATCH /v1/pages/{page_id}/markdown — targeted find-and-replace edits.

        ``content_updates`` is a list (max 100) of
        ``{"old_str", "new_str", "replace_all_matches"?}`` dicts applied in
        order. Far cheaper than read-whole-page-then-rewrite for small edits.
        """
        body = {
            "type": "update_content",
            "update_content": {
                "content_updates": content_updates,
                "allow_deleting_content": allow_deleting_content,
            },
        }
        resp = await self._request(
            "PATCH",
            f"/pages/{page_id}/markdown",
            json=body,
            notion_version=MARKDOWN_NOTION_VERSION,
        )
        return resp.json()

    # ------------------------------------------------------------------
    # Blocks (read + write of page body content)
    # ------------------------------------------------------------------

    async def get_blocks(
        self,
        block_id: str,
        *,
        page_size: int = 100,
        recursive: bool = False,
    ) -> list[dict]:
        """GET /v1/blocks/{block_id}/children — returns child blocks, paginated.

        With ``recursive=True``, descends into every block where
        ``has_children`` is True and attaches the fetched children under
        ``block[block['type']]['children']`` so downstream formatters can
        walk the full tree.

        Capped at MAX_READ_PAGES iterations (~5000 blocks) per call as a
        safety bound.
        """
        all_blocks: list[dict] = []
        cursor: str | None = None
        for _ in range(MAX_READ_PAGES):
            params: dict = {"page_size": page_size}
            if cursor:
                params["start_cursor"] = cursor
            resp = await self._request("GET", f"/blocks/{block_id}/children", params=params)
            data = resp.json()
            all_blocks.extend(data.get("results", []))
            if not data.get("has_more"):
                break
            cursor = data.get("next_cursor")

        if recursive:
            for block in all_blocks:
                if not block.get("has_children"):
                    continue
                children = await self.get_blocks(block["id"], page_size=page_size, recursive=True)
                btype = block.get("type", "")
                if btype:
                    block.setdefault(btype, {})["children"] = children

        return all_blocks

    async def append_blocks(self, block_id: str, children: list[dict]) -> list[dict]:
        """PATCH /v1/blocks/{block_id}/children — append child blocks.

        Accepts arbitrarily large/deep *children*. Splits subtrees deeper
        than 2 levels into deferred follow-ups, then chunks the remaining
        top-level array into 100-block batches per the API's children-array
        cap. Returns the flat list of created top-level blocks (with ids).

        Raises :class:`PartialWriteError` if a batch fails after earlier
        batches succeeded.
        """
        if not children:
            return []

        top, deferred = _split_for_depth(children)
        created: list[dict] = []
        total = len(top)

        for i in range(0, total, MAX_CHILDREN_PER_ARRAY):
            batch = top[i : i + MAX_CHILDREN_PER_ARRAY]
            try:
                resp = await self._request(
                    "PATCH",
                    f"/blocks/{block_id}/children",
                    json={"children": batch},
                )
            except NotionAPIError as e:
                raise PartialWriteError(
                    e,
                    written=len(created),
                    remaining=total - len(created),
                    page_id=block_id,
                ) from e
            created.extend(resp.json().get("results", []))

        if deferred:
            try:
                await self._flush_deferred(created, deferred)
            except NotionAPIError as e:
                raise PartialWriteError(
                    e,
                    written=len(created),
                    remaining=sum(len(kids) for _, kids in deferred),
                    page_id=block_id,
                ) from e

        return created

    async def _flush_deferred(
        self,
        created_top: list[dict],
        deferred: list[tuple[list[int], list[dict]]],
    ) -> None:
        """Append deferred deep children to their parent blocks.

        Each entry in *deferred* is ``(path, children)`` where path indexes
        into *created_top* (and possibly into that block's inline children
        for paths of length > 1). Notion's create/append responses don't
        include nested ids, so for paths longer than 1 we fetch the parent's
        children to resolve the depth-2 block id.
        """
        # Group by top-level index to amortise the per-parent GET.
        by_top: dict[int, list[tuple[list[int], list[dict]]]] = {}
        for path, kids in deferred:
            if not path:
                continue
            by_top.setdefault(path[0], []).append((path[1:], kids))

        for top_idx, entries in by_top.items():
            if top_idx >= len(created_top):
                continue
            top_id = created_top[top_idx]["id"]

            direct: list[list[dict]] = []
            nested: list[tuple[list[int], list[dict]]] = []
            for sub_path, kids in entries:
                if sub_path:
                    nested.append((sub_path, kids))
                else:
                    direct.append(kids)

            for kids in direct:
                await self.append_blocks(top_id, kids)

            if nested:
                depth2_blocks = await self.get_blocks(top_id)
                for sub_path, kids in nested:
                    child_idx = sub_path[0]
                    if child_idx >= len(depth2_blocks):
                        continue
                    child_id = depth2_blocks[child_idx]["id"]
                    await self.append_blocks(child_id, kids)

    async def delete_block(self, block_id: str) -> None:
        """DELETE /v1/blocks/{block_id} — delete (archive) a single block."""
        await self._request("DELETE", f"/blocks/{block_id}")

    # ------------------------------------------------------------------
    # Search (used by setup_dev.py — not used at runtime)
    # ------------------------------------------------------------------

    async def search(self, query: str = "", *, filter: dict | None = None) -> list[dict]:
        """POST /v1/search"""
        body: dict = {}
        if query:
            body["query"] = query
        if filter:
            body["filter"] = filter
        all_results: list[dict] = []
        cursor: str | None = None
        for _ in range(10):
            if cursor:
                body["start_cursor"] = cursor
            resp = await self._request("POST", "/search", json=body)
            data = resp.json()
            all_results.extend(data.get("results", []))
            if not data.get("has_more"):
                break
            cursor = data.get("next_cursor")
        return all_results

    # ------------------------------------------------------------------
    # Get database metadata (used by setup_dev.py)
    # ------------------------------------------------------------------

    async def get_database(self, database_id: str) -> dict:
        """GET /v1/databases/{database_id}"""
        resp = await self._request("GET", f"/databases/{database_id}")
        return resp.json()

    async def get_property_item(
        self, page_id: str, property_id: str, *, page_size: int = 100
    ) -> list[str]:
        """GET /v1/pages/{page_id}/properties/{property_id} — paginated relation
        items, flattened to related-page ids.

        Notion caps relation properties at 25 items on a plain page GET
        (``has_more`` is set on the inline value); this endpoint is the only
        way to fetch the rest. *property_id* is the property's short id
        (``page["properties"][name]["id"]`` from a prior ``get_page`` call),
        not the human-readable property name.
        """
        ids: list[str] = []
        cursor: str | None = None
        for _ in range(MAX_READ_PAGES):
            params: dict = {"page_size": page_size}
            if cursor:
                params["start_cursor"] = cursor
            resp = await self._request(
                "GET", f"/pages/{page_id}/properties/{property_id}", params=params
            )
            data = resp.json()
            for item in data.get("results", []):
                rel_id = item.get("relation", {}).get("id")
                if rel_id:
                    ids.append(rel_id)
            if not data.get("has_more"):
                break
            cursor = data.get("next_cursor")
        return ids

    async def get_data_source(self, ds_id: str) -> dict:
        """GET /v1/data_sources/{ds_id} — returns schema with `properties` map.

        Used at server startup to discover live select options (e.g. Notes
        Type) instead of relying on hardcoded enums.
        """
        resp = await self._request("GET", f"/data_sources/{ds_id}")
        return resp.json()

    async def list_templates(self, ds_id: str) -> list[dict]:
        """GET /v1/data_sources/{ds_id}/templates — list a data source's page
        templates ({id, name, is_default}), paginated. Used so create_project
        can offer/apply a named or default template the same way UB's own
        paid template automation does, rather than inventing new structure.
        """
        all_results: list[dict] = []
        cursor: str | None = None
        for _ in range(10):
            params: dict = {"page_size": 100}
            if cursor:
                params["start_cursor"] = cursor
            resp = await self._request("GET", f"/data_sources/{ds_id}/templates", params=params)
            data = resp.json()
            all_results.extend(data.get("templates", []))
            if not data.get("has_more"):
                break
            cursor = data.get("next_cursor")
        return all_results

    # ------------------------------------------------------------------
    # Database CRUD (Tier 3 of the Hermes extension)
    # ------------------------------------------------------------------
    # These endpoints require the 2026-03-11 Notion API version; the rest of
    # this client stays on 2025-09-03. Each call pins MARKDOWN_NOTION_VERSION
    # explicitly so a future Notion-Version bump doesn't accidentally
    # surface DB-mutation tools to workspaces that haven't opted in.

    async def create_database(
        self,
        *,
        parent_page_id: str,
        title: str,
        properties: dict,
        description: list[dict] | None = None,
        is_inline: bool = False,
        icon: dict | None = None,
        cover: dict | None = None,
    ) -> dict:
        """POST /v1/databases — create a new database under *parent_page_id*.

        ``properties`` is the Notion API shape itself — caller's responsibility
        to construct valid property payloads (e.g. ``{"Name": {"title": {}}}``
        for a title, ``{"Status": {"select": {"options": [...]}}}`` for a
        select, etc.). Returns the created database dict on success.
        """
        body: dict = {
            "parent": {"type": "page_id", "page_id": parent_page_id},
            "title": [{"type": "text", "text": {"content": title}}],
            "properties": properties,
            "is_inline": is_inline,
        }
        if description:
            body["description"] = description
        if icon:
            body["icon"] = icon
        if cover:
            body["cover"] = cover
        resp = await self._request(
            "POST",
            "/databases",
            json=body,
            notion_version=MARKDOWN_NOTION_VERSION,
        )
        return resp.json()

    async def get_database_schema(self, database_id: str) -> dict:
        """GET /v1/databases/{id} then GET /v1/data_sources/{id} for each
        associated data source. Returns ``{database_id, data_sources}`` where
        each data source carries its full schema + id.

        Note: as of Notion 2025-09-03, a database can have multiple data
        sources behind it. Hermes's planned DB scaffolding assumes a
        1:1 layout but tolerates N:1 — the response includes every data
        source so the caller can pick the right schema to mutate.
        """
        db_resp = await self._request("GET", f"/databases/{database_id}")
        db = db_resp.json()
        ds_ids = list(db.get("data_sources") or [])
        if not ds_ids and db.get("data_source_id"):
            ds_ids = [db["data_source_id"]]

        sources = []
        for ds_id in ds_ids:
            ds_resp = await self._request("GET", f"/data_sources/{ds_id}")
            sources.append({"id": ds_id, "schema": ds_resp.json()})

        return {"database_id": database_id, "data_sources": sources}

    async def update_database_schema(self, data_source_id: str, properties: dict) -> dict:
        """PATCH /v1/data_sources/{id} with a ``properties`` dict that can
        ADD/UPDATE/DELETE schema entries.

        Notion's contract: pass ``{"<name>": null}`` to delete a property, or
        ``{"<name>": {"<type>": {...config...}}}`` to add or update one. The
        server returns the patched data source dict on success.
        """
        resp = await self._request(
            "PATCH",
            f"/data_sources/{data_source_id}",
            json={"properties": properties},
            notion_version=MARKDOWN_NOTION_VERSION,
        )
        return resp.json()

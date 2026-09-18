"""FastMCP server with 48 tools for Thomas Frank's Ultimate Brain."""

from __future__ import annotations

import asyncio
import sys
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from typing import Annotated, Literal
from zoneinfo import ZoneInfo

from mcp.server.fastmcp import Context, FastMCP
from mcp.types import ToolAnnotations
from pydantic import BaseModel, Field

from .config import (
    GOAL_STATUSES,
    NOTE_TYPES,
    NOTES_TYPE_PROP,
    PIPELINE_STATUSES,
    PROJECT_STATUSES,
    RECUR_UNITS,
    TAG_TYPES,
    TASK_PRIORITIES,
    TASK_STATUSES,
    UBConfig,
    extract_property_metadata,
    extract_select_options,
)
from .formatters import (
    blocks_to_text,
    format_generic_page,
    format_goal,
    format_milestone,
    format_note,
    format_project,
    format_tag,
    format_task,
    format_work_session,
    text_to_blocks,
)
from .notion_client import NotionAPIError, NotionClient, PartialWriteError

# Cap concurrent block deletes during set_page_content replace mode. Notion
# rate-limits at ~3 req/sec; the per-client limiter already serialises calls,
# but capping concurrency here also prevents thousands of pending coroutines.
_DELETE_CONCURRENCY = 5

# Cap concurrent in-flight task patches during bulk_update_tasks. The
# per-client rate limiter already serialises the actual HTTP calls — this
# bound just stops thousands of pending coroutines from being scheduled at
# once on a very large batch.
_BULK_UPDATE_CONCURRENCY = 10

# Default per-bucket cap on daily_review_snapshot. Bobby's task list won't
# overshoot this in practice, but it bounds the worst-case payload size on a
# very large workspace and surfaces via the per-bucket truncated flag.
_SNAPSHOT_BUCKET_CAP = 100

# ---------------------------------------------------------------------------
# Lifespan context
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class TasksSchema:
    """Live introspection of the Tasks data source — used to construct the
    right Notion property payload for the optional `location` parameter on
    create_task / update_task / bulk_update_tasks, and surfaced verbatim in
    daily_review_snapshot.task_schema so the agent can constrain proposals.
    """

    has_location_property: bool = False
    location_property_name: str | None = None
    location_property_type: str | None = None  # 'select' | 'multi_select' | 'status'
    location_options: tuple[str, ...] = ()
    labels_options: tuple[str, ...] = ()


@dataclass(frozen=True)
class MilestonesSchema:
    """Live introspection of the Milestones data source. Confirmed against a
    real workspace: only Name is guaranteed to exist — Goal relation, Date
    Completed, and Target Deadline are all optional and workspace-dependent,
    so search/create/update_milestone only touch whichever of these are
    actually present rather than assuming the full documented schema.
    """

    goal_property_name: str | None = None  # 'Goal' or 'Goals', whichever exists
    has_date_completed: bool = False
    has_target_deadline: bool = False


@dataclass(frozen=True)
class PeopleSchema:
    """Live introspection of the People data source. Pipeline Status and
    Relationship options are used for input validation on create_person,
    update_person, and search_people. The presence flags let create_person
    silently skip properties a workspace has removed (instead of 400-ing on
    every create). Empty / default values mean discovery failed — writers
    degrade to a Name-only / status-unvalidated path.
    """

    pipeline_status_options: tuple[str, ...] = ()
    relationship_options: tuple[str, ...] = ()
    has_birthday: bool = False
    has_check_in: bool = False
    has_last_check_in: bool = False
    has_email: bool = False
    has_phone: bool = False


@dataclass
class AppContext:
    client: NotionClient
    config: UBConfig
    # Live Notes Type select options. Set once at lifespan startup before
    # yield, read-only thereafter — no locking required.
    note_types: list[str] = field(default_factory=list)
    # "discovered" if populated from the live Notion schema, "fallback" if
    # discovery failed and we fell back to config.NOTE_TYPES.
    note_types_source: Literal["discovered", "fallback"] = "fallback"
    # Live Tasks property schema. Always present; an empty/default value
    # means discovery failed and the location parameter on tools will no-op
    # with a `_warning` field in results.
    tasks_schema: TasksSchema = field(default_factory=TasksSchema)
    # Live Milestones property schema, same best-effort-discovery contract
    # as tasks_schema. Empty/default means Milestones is either unconfigured
    # or only has Name — search/create/update_milestone degrade accordingly.
    milestones_schema: MilestonesSchema = field(default_factory=MilestonesSchema)
    # Live People property schema. Empty/default means People is either
    # unconfigured or discovery failed — search/create/update_person degrade
    # to a Name-only / no-validation path.
    people_schema: PeopleSchema = field(default_factory=PeopleSchema)
    # Whether the page-markdown endpoints (API 2026-03-11) are available.
    # None = unknown (not probed yet); set True on first success, False on first
    # version-unavailable error. Once True, markdown errors are surfaced rather
    # than silently degraded to the block path (a 400 then means a real content
    # error, not "endpoint missing").
    markdown_supported: bool | None = None


@asynccontextmanager
async def app_lifespan(server: FastMCP) -> AsyncIterator[AppContext]:
    config = UBConfig.from_env()
    client = NotionClient(config.notion_secret)
    # Schema discovery is strictly best-effort: it must NEVER prevent the
    # server from starting. A crash here used to take down all 30 tools (the
    # MCP client would connect, then list zero tools). Belt-and-suspenders on
    # top of the per-function guards — any unforeseen error (parsing, network,
    # interpreter quirk) degrades to static fallbacks instead of exit().
    try:
        note_types, note_types_source = await _discover_note_types(client, config)
    except Exception as e:  # noqa: BLE001 — startup must not crash on discovery
        print(
            f"[ultimate-brain-mcp] Notes Type discovery crashed ({e!r}); using static NOTE_TYPES.",
            file=sys.stderr,
        )
        note_types, note_types_source = list(NOTE_TYPES), "fallback"
    try:
        tasks_schema = await _discover_tasks_schema(client, config)
    except Exception as e:  # noqa: BLE001 — startup must not crash on discovery
        print(
            f"[ultimate-brain-mcp] Tasks schema discovery crashed ({e!r}); "
            f"location parameter will no-op for this session.",
            file=sys.stderr,
        )
        tasks_schema = TasksSchema()
    try:
        milestones_schema = await _discover_milestones_schema(client, config)
    except Exception as e:  # noqa: BLE001 — startup must not crash on discovery
        print(
            f"[ultimate-brain-mcp] Milestones schema discovery crashed ({e!r}); "
            f"search/create/update_milestone will only touch Name for this session.",
            file=sys.stderr,
        )
        milestones_schema = MilestonesSchema()
    try:
        people_schema = await _discover_people_schema(client, config)
    except Exception as e:  # noqa: BLE001 — startup must not crash on discovery
        print(
            f"[ultimate-brain-mcp] People schema discovery crashed ({e!r}); "
            f"search/create/update_person will only touch Name for this session.",
            file=sys.stderr,
        )
        people_schema = PeopleSchema()
    try:
        yield AppContext(
            client=client,
            config=config,
            note_types=note_types,
            note_types_source=note_types_source,
            tasks_schema=tasks_schema,
            milestones_schema=milestones_schema,
            people_schema=people_schema,
        )
    finally:
        await client.close()


async def _discover_note_types(
    client: NotionClient, config: UBConfig
) -> tuple[list[str], Literal["discovered", "fallback"]]:
    """Fetch live Type select options from the Notes data source.

    Falls back to config.NOTE_TYPES on any failure — including an empty
    discovered list, which would otherwise silently break every note tool
    call for the session (see ISC-35).
    """
    try:
        schema = await client.get_data_source(config.notes_ds_id)
        options = extract_select_options(schema, NOTES_TYPE_PROP)
    except Exception as e:  # noqa: BLE001 — discovery is best-effort
        print(
            f"[ultimate-brain-mcp] Notes Type discovery failed ({e!r}); "
            f"falling back to static NOTE_TYPES.",
            file=sys.stderr,
        )
        return list(NOTE_TYPES), "fallback"

    if not options:
        print(
            f"[ultimate-brain-mcp] Notes data source has no '{NOTES_TYPE_PROP}' "
            f"select options; falling back to static NOTE_TYPES.",
            file=sys.stderr,
        )
        return list(NOTE_TYPES), "fallback"
    return options, "discovered"


async def _discover_tasks_schema(client: NotionClient, config: UBConfig) -> TasksSchema:
    """Introspect the Tasks data source for Location + Labels metadata.

    Only inspects properties relevant to the workflows that need this
    metadata — the snapshot exposes location + labels options to the agent so
    it can propose values from the live, valid set.

    Falls back to a default ``TasksSchema()`` (has_location_property=False,
    empty labels_options) on any error. The location parameter on writer
    tools no-ops with a ``_warning`` field when the schema is empty.
    """
    try:
        schema = await client.get_data_source(config.tasks_ds_id)
        location_meta = extract_property_metadata(schema, "Location")
        labels_meta = extract_property_metadata(schema, "Labels")
    except Exception as e:  # noqa: BLE001 — discovery is best-effort
        print(
            f"[ultimate-brain-mcp] Tasks schema discovery failed ({e!r}); "
            f"location parameter will no-op for this session.",
            file=sys.stderr,
        )
        return TasksSchema()

    return TasksSchema(
        has_location_property=bool(location_meta.get("exists")),
        location_property_name=location_meta.get("name") if location_meta.get("exists") else None,
        location_property_type=location_meta.get("type") if location_meta.get("exists") else None,
        location_options=tuple(location_meta.get("options", []) or ()),
        labels_options=tuple(labels_meta.get("options", []) or ()),
    )


async def _discover_milestones_schema(client: NotionClient, config: UBConfig) -> MilestonesSchema:
    """Introspect the Milestones data source (if configured) for the Goal
    relation, Date Completed, and Target Deadline properties. Live-verified
    against a real workspace where Milestones only has Name populated —
    these are genuinely optional, not just usually-empty, so this discovers
    presence rather than assuming the fully-documented schema.

    Falls back to an empty ``MilestonesSchema()`` (Name-only) if Milestones
    isn't configured or discovery fails for any reason.
    """
    ds_id = config.secondary_ds.get("Milestones")
    if not ds_id:
        return MilestonesSchema()
    try:
        schema = await client.get_data_source(ds_id)
    except Exception as e:  # noqa: BLE001 — discovery is best-effort
        print(
            f"[ultimate-brain-mcp] Milestones schema fetch failed ({e!r}); "
            f"falling back to Name-only.",
            file=sys.stderr,
        )
        return MilestonesSchema()

    goal_meta = extract_property_metadata(schema, "Goal")
    if not goal_meta.get("exists"):
        goal_meta = extract_property_metadata(schema, "Goals")
    date_completed_meta = extract_property_metadata(schema, "Date Completed")
    target_deadline_meta = extract_property_metadata(schema, "Target Deadline")

    return MilestonesSchema(
        goal_property_name=goal_meta.get("name") if goal_meta.get("exists") else None,
        has_date_completed=bool(date_completed_meta.get("exists")),
        has_target_deadline=bool(target_deadline_meta.get("exists")),
    )


async def _discover_people_schema(client: NotionClient, config: UBConfig) -> PeopleSchema:
    """Introspect the People data source (if configured) for Pipeline Status
    options, Relationship options, and the presence of optional date /
    contact properties (Birthday, Check-In, Last Check-In, Email, Phone).

    The live options list is what callers will validate pipeline_status
    against; if Notion rejects a value, that's a schema drift we don't try
    to paper over here. Presence flags let create_person / update_person
    silently skip properties a workspace has deleted — important because
    People is more workspace-customised than Tasks.

    Falls back to an empty ``PeopleSchema()`` (Name-only) if People isn't
    configured or discovery fails for any reason.
    """
    ds_id = config.secondary_ds.get("People")
    if not ds_id:
        return PeopleSchema()
    try:
        schema = await client.get_data_source(ds_id)
    except Exception as e:  # noqa: BLE001 — discovery is best-effort
        print(
            f"[ultimate-brain-mcp] People schema fetch failed ({e!r}); falling back to Name-only.",
            file=sys.stderr,
        )
        return PeopleSchema()

    props = schema.get("properties", {})
    pipeline_meta = extract_property_metadata(schema, "Pipeline Status")
    relationship_meta = extract_property_metadata(schema, "Relationship")

    # Fall back to canonical pipeline_status options if the live discovery
    # found nothing — keeps validation tight when the schema is partially
    # customisable.
    pipeline_options = pipeline_meta.get("options") or list(PIPELINE_STATUSES)

    def _has(name: str) -> bool:
        return name in props

    return PeopleSchema(
        pipeline_status_options=tuple(pipeline_options),
        relationship_options=tuple(relationship_meta.get("options", []) or ()),
        has_birthday=_has("Birthday"),
        has_check_in=_has("Check-In"),
        has_last_check_in=_has("Last Check-In"),
        has_email=_has("Email"),
        has_phone=_has("Phone"),
    )


mcp = FastMCP(
    "Ultimate Brain",
    instructions=(
        "Tools for managing Thomas Frank's Ultimate Brain Notion system. "
        "Covers Tasks, Projects, Notes, Tags, and Goals using the PARA methodology. "
        "All search tools default to showing active/non-archived items. "
        "Use daily_summary for a quick count-only overview. "
        "For a full daily review (per-task details across completed / overdue / "
        "due-tomorrow / on-My-Day / inbox), call daily_review_snapshot — one "
        "call returns everything plus the project and area-tag lookup tables. "
        "For batch task updates (≥3 tasks), call bulk_update_tasks instead of "
        "looping update_task — single round-trip with per-row results."
    ),
    lifespan=app_lifespan,
)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _ctx(ctx: Context | None) -> AppContext:
    # FastMCP injects ctx at call time, so it is never None in practice; the
    # `Context | None` annotation only reflects the `ctx: Context = None`
    # sentinel default on the tool signatures.
    assert ctx is not None
    return ctx.request_context.lifespan_context


def _today() -> str:
    return date.today().isoformat()


def _error(msg: str) -> dict:
    return {"error": msg}


def _validate_note_type(app: AppContext, note_type: str | None) -> dict | None:
    """Reject `note_type` not in the live discovered options.

    Validation is a case-sensitive exact match — `"meeting"` does not match
    `"Meeting"`. The error message lists the live valid set so an AI client
    can self-correct on the next call.
    """
    if note_type is None:
        return None
    if note_type in app.note_types:
        return None
    return _error(
        f"Invalid note_type {note_type!r}. Valid options "
        f"(source: {app.note_types_source}): {app.note_types}"
    )


def _validate_pipeline_status(app: AppContext, value: str | None) -> dict | None:
    """Reject `pipeline_status` not in the live discovered options.

    Mirrors `_validate_note_type`'s case-sensitive exact-match policy. Falls
    through (no validation) when the live options list is empty — discovery
    failed and we don't want to block writes for a session that started
    without introspection.
    """
    if value is None:
        return None
    if not app.people_schema.pipeline_status_options:
        return None
    if value in app.people_schema.pipeline_status_options:
        return None
    return _error(
        f"Invalid pipeline_status {value!r}. Valid options: "
        f"{list(app.people_schema.pipeline_status_options)}"
    )


def _handle_api_error(e: NotionAPIError, hint: str = "") -> dict:
    if e.status == 404:
        msg = f"Page not found. {hint}" if hint else "Page not found."
    elif e.status == 400:
        msg = f"Invalid request: {e}. {hint}" if hint else f"Invalid request: {e}"
    elif e.status == 401:
        msg = "Authentication failed. Check NOTION_INTEGRATION_SECRET."
    elif e.status == 403:
        msg = "Permission denied. Make sure the page is shared with the integration."
    else:
        msg = f"Notion API error ({e.status}): {e}"
    err: dict = {"error": msg}
    if isinstance(e, PartialWriteError):
        err["partial_write"] = {
            "blocks_written": e.written,
            "blocks_remaining": e.remaining,
            "page_id": e.page_id,
        }
    return err


async def _resolve_truncated_relations(app: AppContext, page: dict, result: dict) -> None:
    """Mutate *result* in place: for each property name flagged in
    ``result["_truncated_relations"]`` (set by ``_annotate_truncation`` when a
    relation hits Notion's 25-item inline cap), fetch the full related-page id
    list via the paginated property endpoint and attach it under
    ``result["_resolved_relations"][name]``. Any property that still can't be
    resolved (missing property id, API error) stays listed in
    ``_truncated_relations``; fully-resolved properties are removed from it.
    """
    truncated = result.get("_truncated_relations")
    if not truncated:
        return
    props = page.get("properties", {})
    page_id = page.get("id", "")
    resolved: dict[str, list[str]] = {}
    still_truncated: list[str] = []
    for name in truncated:
        prop_id = props.get(name, {}).get("id")
        if not prop_id:
            still_truncated.append(name)
            continue
        try:
            resolved[name] = await app.client.get_property_item(page_id, prop_id)
        except NotionAPIError:
            still_truncated.append(name)
    if resolved:
        result["_resolved_relations"] = resolved
    if still_truncated:
        result["_truncated_relations"] = still_truncated
    else:
        result.pop("_truncated_relations", None)


async def _find_possible_duplicate(
    app: AppContext, ds_id: str, name: str, formatter
) -> dict | None:
    """Look for an existing, non-archived item with the exact same title.

    Warning-only, never blocking — surfaced on create_task/create_note as
    `possible_duplicate` so the caller (human or agent) can decide whether
    to proceed. Errors during the check are swallowed: a failed duplicate
    check should never prevent the actual create from happening.
    """
    try:
        pages = await app.client.query_all(
            ds_id,
            filter={"property": "Name", "title": {"equals": name}},
            max_pages=1,
        )
    except NotionAPIError:
        return None
    if not pages:
        return None
    dup = formatter(pages[0])
    return {"id": dup.get("id"), "name": dup.get("name"), "url": dup.get("url")}


async def _bounded_gather(coros: list, *, limit: int = _DELETE_CONCURRENCY) -> None:
    """Run *coros* with at most *limit* concurrent in flight."""
    sem = asyncio.Semaphore(limit)

    async def _run(coro):
        async with sem:
            await coro

    await asyncio.gather(*(_run(c) for c in coros))


# ---------------------------------------------------------------------------
# Property builders — construct Notion property values
# ---------------------------------------------------------------------------


def _prop_title(text: str) -> dict:
    return {"title": [{"text": {"content": text}}]}


def _prop_rich_text(text: str) -> dict:
    return {"rich_text": [{"text": {"content": text}}]}


def _prop_select(name: str) -> dict:
    return {"select": {"name": name}}


def _prop_multi_select(names: list[str]) -> dict:
    return {"multi_select": [{"name": n} for n in names]}


def _prop_status(name: str) -> dict:
    return {"status": {"name": name}}


def _prop_date(start: str, end: str | None = None) -> dict:
    d: dict = {"start": start}
    if end:
        d["end"] = end
    return {"date": d}


def _prop_checkbox(checked: bool) -> dict:
    return {"checkbox": checked}


def _prop_number(value: float) -> dict:
    return {"number": value}


def _prop_url(url: str) -> dict:
    return {"url": url}


def _prop_email(value: str) -> dict:
    return {"email": value}


def _prop_phone_number(value: str) -> dict:
    return {"phone_number": value}


def _prop_relation(ids: list[str]) -> dict:
    return {"relation": [{"id": i} for i in ids]}


def _build_location_payload(schema: TasksSchema, value: str) -> tuple[dict | None, str | None]:
    """Construct the Notion property payload for the ``location`` parameter.

    Returns ``(payload, warning)``:

    - ``(payload_dict, None)`` when the Tasks data source has a Location
      property. The payload shape matches the discovered property type
      (``select``, ``status``, or ``multi_select``). Caller assigns it
      under ``schema.location_property_name``.
    - ``(None, "<actionable message>")`` when no Location property exists.
      Caller surfaces the warning on the result so the agent learns where
      to look (typically the snapshot's ``task_schema.has_location_property``).
    """
    if not schema.has_location_property or not schema.location_property_type:
        return None, (
            "location ignored: Tasks has no Location property. "
            "Check daily_review_snapshot.task_schema.has_location_property; "
            "if locations live in Labels, pass them via the labels parameter instead."
        )
    ptype = schema.location_property_type
    if ptype == "select":
        return _prop_select(value), None
    if ptype == "status":
        return _prop_status(value), None
    if ptype == "multi_select":
        return _prop_multi_select([value]), None
    return None, (
        f"location ignored: unsupported Location property type {ptype!r}. "
        f"Supported types: select, status, multi_select."
    )


# =========================================================================
#  TASKS (6 tools)
# =========================================================================


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def search_tasks(
    status: Annotated[
        str | None,
        Field(
            description=f"Filter by status. Options: {', '.join(TASK_STATUSES)}. Omit for non-Done tasks."
        ),
    ] = None,
    project_id: Annotated[
        str | None,
        Field(description="Filter by project page ID."),
    ] = None,
    priority: Annotated[
        str | None,
        Field(description=f"Filter by priority. Options: {', '.join(TASK_PRIORITIES)}."),
    ] = None,
    due_before: Annotated[
        str | None,
        Field(description="Due date on or before this date (YYYY-MM-DD), e.g. '2026-05-09'."),
    ] = None,
    my_day: Annotated[
        bool | None,
        Field(description="Filter to My Day tasks only."),
    ] = None,
    query: Annotated[
        str | None,
        Field(description="Text to search for in task names."),
    ] = None,
    due_after: Annotated[
        str | None,
        Field(
            description="Due date on or after this date (YYYY-MM-DD). Combine with due_before for a range."
        ),
    ] = None,
    due_on: Annotated[
        str | None,
        Field(
            description=(
                "Due date equals this single day (YYYY-MM-DD). Mutually exclusive with "
                "due_before and due_after — use those for ranges, this for a single day."
            )
        ),
    ] = None,
    parent_task_id: Annotated[
        str | None,
        Field(description="Filter by parent task page ID (subtasks of a specific task)."),
    ] = None,
    label: Annotated[
        str | None,
        Field(description="Filter by label name (matches tasks tagged with this label)."),
    ] = None,
    completed_before: Annotated[
        str | None,
        Field(
            description="Completion date on or before this date (YYYY-MM-DD). Best combined with status='Done'."
        ),
    ] = None,
    completed_after: Annotated[
        str | None,
        Field(
            description="Completion date on or after this date (YYYY-MM-DD). Best combined with status='Done'."
        ),
    ] = None,
    limit: Annotated[
        int,
        Field(description="Maximum results to return.", ge=1, le=100),
    ] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """Search tasks by name, status, project, priority, due date, labels, parent task, or completion date.
    Defaults to non-Done tasks. Combine due_before + due_after for date ranges, or due_on for a single day.
    For My Day tasks specifically, use get_my_day. For unprocessed tasks, use get_inbox_tasks.
    For a full daily-review payload (multiple buckets in one call), use daily_review_snapshot."""
    app = _ctx(ctx)
    if due_on is not None and (due_before is not None or due_after is not None):
        return _error(
            "due_on cannot combine with due_before or due_after. "
            "Use due_on for a single day, or the pair for a range."
        )
    filters: list[dict] = []

    if status:
        filters.append({"property": "Status", "status": {"equals": status}})
    else:
        filters.append({"property": "Status", "status": {"does_not_equal": "Done"}})

    if project_id:
        filters.append({"property": "Project", "relation": {"contains": project_id}})
    if priority:
        filters.append({"property": "Priority", "status": {"equals": priority}})
    if due_before:
        filters.append({"property": "Due", "date": {"on_or_before": due_before}})
    if my_day is True:
        filters.append({"property": "My Day", "checkbox": {"equals": True}})
    if query:
        filters.append({"property": "Name", "title": {"contains": query}})
    if due_after:
        filters.append({"property": "Due", "date": {"on_or_after": due_after}})
    if due_on:
        filters.append({"property": "Due", "date": {"equals": due_on}})
    if parent_task_id:
        filters.append({"property": "Parent Task", "relation": {"contains": parent_task_id}})
    if label:
        filters.append({"property": "Labels", "multi_select": {"contains": label}})
    if completed_before:
        filters.append({"property": "Completed", "date": {"on_or_before": completed_before}})
    if completed_after:
        filters.append({"property": "Completed", "date": {"on_or_after": completed_after}})

    query_filter = {"and": filters} if len(filters) > 1 else filters[0] if filters else None
    sorts = [{"property": "Due", "direction": "ascending"}]

    try:
        pages = await app.client.query_all(app.config.tasks_ds_id, filter=query_filter, sorts=sorts)
        loc_name = app.tasks_schema.location_property_name
        return [format_task(p, location_property_name=loc_name) for p in pages[:limit]]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_my_day(
    ctx: Context = None,
) -> list[dict] | dict:
    """Get all non-Done tasks with My Day checked, sorted by priority.
    Returns task name, status, priority, and due date."""
    app = _ctx(ctx)
    query_filter = {
        "and": [
            {"property": "My Day", "checkbox": {"equals": True}},
            {"property": "Status", "status": {"does_not_equal": "Done"}},
        ]
    }
    try:
        pages = await app.client.query_all(app.config.tasks_ds_id, filter=query_filter)
        loc_name = app.tasks_schema.location_property_name
        tasks = [format_task(p, location_property_name=loc_name) for p in pages]
        priority_order = {"High": 0, "Medium": 1, "Low": 2, None: 3}
        tasks.sort(key=lambda t: priority_order.get(t.get("priority"), 3))
        return tasks
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_inbox_tasks(
    ctx: Context = None,
) -> list[dict] | dict:
    """Get unprocessed inbox tasks: status is To Do, no project assigned, no due date.
    These need to be triaged — assign a project, due date, or move to a different status."""
    app = _ctx(ctx)
    query_filter = {
        "and": [
            {"property": "Status", "status": {"equals": "To Do"}},
            {"property": "Project", "relation": {"is_empty": True}},
            {"property": "Due", "date": {"is_empty": True}},
        ]
    }
    try:
        pages = await app.client.query_all(app.config.tasks_ds_id, filter=query_filter)
        loc_name = app.tasks_schema.location_property_name
        return [format_task(p, location_property_name=loc_name) for p in pages]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def create_task(
    name: Annotated[str, Field(description="Task name.")],
    status: Annotated[
        str | None,
        Field(description=f"Status. Options: {', '.join(TASK_STATUSES)}. Defaults to To Do."),
    ] = None,
    due: Annotated[
        str | None,
        Field(
            description=(
                "Due date, YYYY-MM-DD (e.g. '2026-05-09'). For time-blocking, pass a full "
                "ISO 8601 datetime instead (e.g. '2026-05-09T09:00:00+05:30') and pair with "
                "due_end to set a specific time slot."
            )
        ),
    ] = None,
    due_end: Annotated[
        str | None,
        Field(
            description=(
                "End of the time block, ISO 8601 datetime (e.g. '2026-05-09T10:30:00+05:30'). "
                "Only meaningful when due includes a time — sets Due as a date range for "
                "time-blocking. Ignored if due is not also set."
            )
        ),
    ] = None,
    priority: Annotated[
        str | None,
        Field(description=f"Priority. Options: {', '.join(TASK_PRIORITIES)}."),
    ] = None,
    project_id: Annotated[
        str | None,
        Field(description="Project page ID to link this task to."),
    ] = None,
    labels: Annotated[
        list[str] | None,
        Field(description="Label names (multi-select)."),
    ] = None,
    my_day: Annotated[
        bool,
        Field(description="Add to My Day."),
    ] = False,
    parent_task_id: Annotated[
        str | None,
        Field(description="Parent task page ID (for sub-tasks)."),
    ] = None,
    tag_ids: Annotated[
        list[str] | None,
        Field(
            description=(
                "Tag page IDs to link via the Tag relation (PARA Area / Resource / Entity). "
                "Use search_tags to find IDs. Distinct from labels (multi-select strings)."
            )
        ),
    ] = None,
    location: Annotated[
        str | None,
        Field(
            description=(
                "Sets the Tasks Location property. Auto-detects select / multi_select / status type. "
                "Only valid when Tasks has a Location property — check "
                "daily_review_snapshot.task_schema.has_location_property first. "
                "If location lives in Labels in this workspace, pass it via labels=[...] instead."
            )
        ),
    ] = None,
    enforce_schedule: Annotated[
        bool | None,
        Field(
            description=(
                "Sets the Enforce Schedule checkbox for recurring tasks (keeps the due "
                "date on a fixed cadence instead of shifting from completion date)."
            )
        ),
    ] = None,
    recur_interval: Annotated[
        int | None,
        Field(description="Recur Interval (number). Pairs with recur_unit."),
    ] = None,
    recur_unit: Annotated[
        str | None,
        Field(
            description=(
                f"Recur Unit (select). Canonical options: {', '.join(RECUR_UNITS)}. "
                "Custom workspace options also accepted — Notion returns 400 on bad values."
            )
        ),
    ] = None,
    days: Annotated[
        list[str] | None,
        Field(
            description=(
                "Days (multi_select of weekday short names, e.g. ['Mon', 'Wed']). "
                "Used together with recur_unit='Week(s)' or similar."
            )
        ),
    ] = None,
    snooze_until: Annotated[
        str | None,
        Field(
            description=(
                "Snooze Until (YYYY-MM-DD). Shifts Due to that date and sets the "
                "Snooze Until property. If both `due` and `snooze_until` are passed, "
                "snooze_until wins (last write)."
            )
        ),
    ] = None,
    content: Annotated[
        str | None,
        Field(
            description=(
                "Page body content as markdown. Supports: # headings, - bullets, "
                "1. numbered lists, - [ ] to-dos, ```code blocks```, > quotes, --- dividers, "
                "and plain paragraphs."
            )
        ),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Create a new task. Only name is required. Use search_projects to find project IDs.

    For batches of 3+ task creations or updates, prefer bulk_update_tasks (updates only)
    or bulk_create_tasks (creates only).
    Pure creates still go through this tool one at a time.
    """
    app = _ctx(ctx)
    props: dict = {"Name": _prop_title(name)}
    location_warning: str | None = None

    if status:
        props["Status"] = _prop_status(status)
    if due:
        props["Due"] = _prop_date(due, due_end)
    if priority:
        props["Priority"] = _prop_status(priority)
    if project_id:
        props["Project"] = _prop_relation([project_id])
    if labels:
        props["Labels"] = _prop_multi_select(labels)
    if my_day:
        props["My Day"] = _prop_checkbox(True)
    if parent_task_id:
        props["Parent Task"] = _prop_relation([parent_task_id])
    if tag_ids:
        props["Tag"] = _prop_relation(tag_ids)
    if enforce_schedule is not None:
        props["Enforce Schedule"] = _prop_checkbox(enforce_schedule)
    if recur_interval is not None:
        props["Recur Interval"] = _prop_number(recur_interval)
    if recur_unit is not None:
        props["Recur Unit"] = _prop_select(recur_unit)
    if days:
        props["Days"] = _prop_multi_select(days)
    if snooze_until:
        props["Due"] = _prop_date(snooze_until)
        props["Snooze Until"] = _prop_date(snooze_until)
    if location is not None:
        payload, warning = _build_location_payload(app.tasks_schema, location)
        if payload is not None and app.tasks_schema.location_property_name:
            props[app.tasks_schema.location_property_name] = payload
        if warning:
            location_warning = warning

    children = text_to_blocks(content) if content else None

    possible_duplicate = await _find_possible_duplicate(
        app, app.config.tasks_ds_id, name, format_task
    )

    try:
        page = await app.client.create_page(app.config.tasks_ds_id, props, children=children)
        result = format_task(page, location_property_name=app.tasks_schema.location_property_name)
        if location_warning:
            result["_warning"] = location_warning
        if possible_duplicate:
            result["possible_duplicate"] = possible_duplicate
        return result
    except NotionAPIError as e:
        return _handle_api_error(e, "Check that project/parent IDs are valid.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def update_task(
    task_id: Annotated[str, Field(description="Task page ID to update.")],
    name: Annotated[str | None, Field(description="New task name.")] = None,
    status: Annotated[
        str | None,
        Field(description=f"New status. Options: {', '.join(TASK_STATUSES)}."),
    ] = None,
    due: Annotated[
        str | None,
        Field(
            description=(
                "New due date, YYYY-MM-DD, or a full ISO 8601 datetime for time-blocking "
                "(e.g. '2026-05-09T09:00:00+05:30'), paired with due_end."
            )
        ),
    ] = None,
    due_end: Annotated[
        str | None,
        Field(
            description=(
                "End of the time block, ISO 8601 datetime. Only meaningful when due includes "
                "a time. Ignored if due is not also set."
            )
        ),
    ] = None,
    priority: Annotated[
        str | None,
        Field(description=f"New priority. Options: {', '.join(TASK_PRIORITIES)}."),
    ] = None,
    project_id: Annotated[str | None, Field(description="New project page ID.")] = None,
    labels: Annotated[
        list[str] | None, Field(description="New labels (replaces existing).")
    ] = None,
    my_day: Annotated[bool | None, Field(description="Set My Day flag.")] = None,
    parent_task_id: Annotated[
        str | None,
        Field(description="New parent task page ID (for sub-tasks)."),
    ] = None,
    tag_ids: Annotated[
        list[str] | None,
        Field(
            description=(
                "New Tag relation IDs (replaces existing). Use search_tags to find IDs. "
                "Distinct from labels (multi-select strings)."
            )
        ),
    ] = None,
    location: Annotated[
        str | None,
        Field(
            description=(
                "Sets the Tasks Location property. Auto-detects select / multi_select / status type. "
                "Only valid when Tasks has a Location property — check "
                "daily_review_snapshot.task_schema.has_location_property first. "
                "If location lives in Labels in this workspace, pass it via labels=[...] instead."
            )
        ),
    ] = None,
    enforce_schedule: Annotated[
        bool | None,
        Field(
            description=(
                "Sets the Enforce Schedule checkbox for recurring tasks (keeps the due "
                "date on a fixed cadence instead of shifting from completion date)."
            )
        ),
    ] = None,
    recur_interval: Annotated[
        int | None,
        Field(description="Recur Interval (number). Pairs with recur_unit."),
    ] = None,
    recur_unit: Annotated[
        str | None,
        Field(
            description=(
                f"Recur Unit (select). Canonical options: {', '.join(RECUR_UNITS)}. "
                "Custom workspace options also accepted — Notion returns 400 on bad values."
            )
        ),
    ] = None,
    days: Annotated[
        list[str] | None,
        Field(
            description=(
                "Days (multi_select of weekday short names, e.g. ['Mon', 'Wed']). "
                "Used together with recur_unit='Week(s)' or similar."
            )
        ),
    ] = None,
    snooze_until: Annotated[
        str | None,
        Field(
            description=(
                "Snooze Until (YYYY-MM-DD). Shifts Due to that date and sets the "
                "Snooze Until property. If both `due` and `snooze_until` are passed, "
                "snooze_until wins (last write)."
            )
        ),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Update any task properties. Only provided fields are changed.
    Use search_tasks to find task IDs. For completing tasks, use complete_task instead.

    For batch updates of 3+ tasks, use bulk_update_tasks instead — single round-trip
    with per-row results vs N separate calls.
    """
    app = _ctx(ctx)
    props: dict = {}
    location_warning: str | None = None
    if name is not None:
        props["Name"] = _prop_title(name)
    if status is not None:
        props["Status"] = _prop_status(status)
    if due is not None:
        props["Due"] = _prop_date(due, due_end)
    if priority is not None:
        props["Priority"] = _prop_status(priority)
    if project_id is not None:
        props["Project"] = _prop_relation([project_id])
    if labels is not None:
        props["Labels"] = _prop_multi_select(labels)
    if my_day is not None:
        props["My Day"] = _prop_checkbox(my_day)
    if parent_task_id is not None:
        props["Parent Task"] = _prop_relation([parent_task_id])
    if tag_ids is not None:
        props["Tag"] = _prop_relation(tag_ids)
    if enforce_schedule is not None:
        props["Enforce Schedule"] = _prop_checkbox(enforce_schedule)
    if recur_interval is not None:
        props["Recur Interval"] = _prop_number(recur_interval)
    if recur_unit is not None:
        props["Recur Unit"] = _prop_select(recur_unit)
    if days is not None:
        props["Days"] = _prop_multi_select(days)
    if snooze_until is not None:
        props["Due"] = _prop_date(snooze_until)
        props["Snooze Until"] = _prop_date(snooze_until)
    if location is not None:
        payload, warning = _build_location_payload(app.tasks_schema, location)
        if payload is not None and app.tasks_schema.location_property_name:
            props[app.tasks_schema.location_property_name] = payload
        if warning:
            location_warning = warning

    if not props:
        return _error("No properties to update. Provide at least one field.")

    try:
        page = await app.client.update_page(task_id, props)
        result = format_task(page, location_property_name=app.tasks_schema.location_property_name)
        if location_warning:
            result["_warning"] = location_warning
        return result
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_tasks to find valid task IDs.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def complete_task(
    task_id: Annotated[str, Field(description="Task page ID to complete.")],
    ctx: Context = None,
) -> dict:
    """Mark a task as Done and set completion date to today.
    Handles recurring tasks: resets status to To Do and advances due date by the recurrence interval.
    Use search_tasks to find task IDs."""
    app = _ctx(ctx)
    loc_name = app.tasks_schema.location_property_name
    try:
        page = await app.client.get_page(task_id)
        task = format_task(page, location_property_name=loc_name)

        # Check for recurrence
        recurrence = task.get("recurrence", "")
        if recurrence:
            # Recurring task: reset to To Do and advance due date. Prefer
            # Notion's own `Next Due` formula — it already understands every
            # recur unit (Nth weekday of month, last day/weekday, Days-based
            # weekday recurrence), which the day/week/month fallback below
            # cannot parse and would otherwise silently mis-advance.
            current_due = task.get("due")
            new_due = task.get("next_due") or (
                _advance_date(current_due, recurrence) if current_due else None
            )
            props: dict = {"Status": _prop_status("To Do")}
            if new_due:
                # Preserve the original time-block (due_end) so a recurring routine
                # keeps its slot length when it advances. task.get("due_end") is
                # absent when the original Due was a single date, so _prop_date
                # produces the same shape as before.
                props["Due"] = _prop_date(new_due, task.get("due_end"))
            props["My Day"] = _prop_checkbox(False)
            page = await app.client.update_page(task_id, props)
            result = format_task(page, location_property_name=loc_name)
            if new_due:
                result["_note"] = f"Recurring task reset. Next due: {new_due}"
            else:
                result["_warning"] = (
                    "Recurring task reset, but the next due date could not be "
                    "determined (no Next Due formula value and the recurrence "
                    "pattern wasn't a simple day/week/month interval). Due date "
                    "left unchanged — check the task in Notion."
                )
            return result
        else:
            # Non-recurring: mark Done
            props = {
                "Status": _prop_status("Done"),
                "Completed": _prop_date(_today()),
                "My Day": _prop_checkbox(False),
            }
            page = await app.client.update_page(task_id, props)
            return format_task(page, location_property_name=loc_name)
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_tasks to find valid task IDs.")


def _advance_date(current: str, recurrence: str) -> str | None:
    """Advance a date by the recurrence interval. Supports 'every N days/weeks/months'.

    Fallback path only — complete_task prefers Notion's own `Next Due` formula,
    which correctly handles every recur unit (Nth weekday of month, last day/
    weekday, Days-based weekday recurrence). This function only understands
    simple day/week/month intervals and returns None rather than guessing when
    it can't parse the pattern — silently defaulting to "+1 week" previously
    produced a wrong due date for any advanced recur unit.
    """
    try:
        dt = datetime.fromisoformat(current)
    except (ValueError, TypeError):
        return None

    rec = recurrence.lower().strip()
    # Parse patterns like "every 3 days", "every week", "every 2 weeks", "every month"
    import re

    match = re.match(r"every\s+(\d+)?\s*(day|week|month)s?", rec)
    if not match:
        return None

    n = int(match.group(1)) if match.group(1) else 1
    unit = match.group(2)

    if unit == "day":
        new_dt = dt + timedelta(days=n)
    elif unit == "week":
        new_dt = dt + timedelta(weeks=n)
    else:  # month — approximate: add 30 days per month
        new_dt = dt + timedelta(days=30 * n)

    return new_dt.date().isoformat()


# =========================================================================
#  PROJECTS (4 tools)
# =========================================================================


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def search_projects(
    status: Annotated[
        str | None,
        Field(
            description=f"Filter by status. Options: {', '.join(PROJECT_STATUSES)}. Omit for active projects (Doing + Ongoing)."
        ),
    ] = None,
    tag_id: Annotated[
        str | None,
        Field(description="Filter by tag page ID."),
    ] = None,
    query: Annotated[
        str | None,
        Field(description="Text to search for in project names."),
    ] = None,
    goal_id: Annotated[
        str | None,
        Field(description="Filter by goal page ID."),
    ] = None,
    deadline_before: Annotated[
        str | None,
        Field(description="Target deadline on or before this date (YYYY-MM-DD)."),
    ] = None,
    deadline_after: Annotated[
        str | None,
        Field(description="Target deadline on or after this date (YYYY-MM-DD)."),
    ] = None,
    completed_before: Annotated[
        str | None,
        Field(
            description="Completion date on or before this date (YYYY-MM-DD). Best combined with status='Complete'."
        ),
    ] = None,
    completed_after: Annotated[
        str | None,
        Field(
            description="Completion date on or after this date (YYYY-MM-DD). Best combined with status='Complete'."
        ),
    ] = None,
    archived: Annotated[
        bool | None,
        Field(description="Filter by archived flag."),
    ] = None,
    limit: Annotated[
        int,
        Field(description="Maximum results to return.", ge=1, le=100),
    ] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """Search projects by name, status, tag, goal, deadline, completion date, or archived flag.
    Defaults to active projects (Doing + Ongoing). Combine deadline_before + deadline_after for date ranges.
    For a full project breakdown with tasks, use get_project_detail instead."""
    app = _ctx(ctx)
    filters: list[dict] = []

    if status:
        filters.append({"property": "Status", "status": {"equals": status}})
    else:
        filters.append(
            {
                "or": [
                    {"property": "Status", "status": {"equals": "Doing"}},
                    {"property": "Status", "status": {"equals": "Ongoing"}},
                ]
            }
        )

    if tag_id:
        filters.append({"property": "Tag", "relation": {"contains": tag_id}})
    if query:
        filters.append({"property": "Name", "title": {"contains": query}})
    if goal_id:
        filters.append({"property": "Goal", "relation": {"contains": goal_id}})
    if deadline_before:
        filters.append({"property": "Target Deadline", "date": {"on_or_before": deadline_before}})
    if deadline_after:
        filters.append({"property": "Target Deadline", "date": {"on_or_after": deadline_after}})
    if completed_before:
        filters.append({"property": "Completed", "date": {"on_or_before": completed_before}})
    if completed_after:
        filters.append({"property": "Completed", "date": {"on_or_after": completed_after}})
    if archived is not None:
        filters.append({"property": "Archived", "checkbox": {"equals": archived}})

    query_filter = {"and": filters} if len(filters) > 1 else filters[0] if filters else None
    sorts = [{"property": "Target Deadline", "direction": "ascending"}]

    try:
        pages = await app.client.query_all(
            app.config.projects_ds_id, filter=query_filter, sorts=sorts
        )
        return [format_project(p) for p in pages[:limit]]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_project_detail(
    project_id: Annotated[str, Field(description="Project page ID.")],
    resolve_relations: Annotated[
        bool,
        Field(
            description=(
                "If any relation on the project (e.g. Tag) is truncated at Notion's "
                "25-item inline cap (see _truncated_relations), fetch the full list "
                "via an extra paginated API call instead of leaving it flagged."
            )
        ),
    ] = False,
    ctx: Context = None,
) -> dict:
    """Get a consolidated project view: properties, task breakdown by status, and recent notes.
    Use search_projects to find project IDs."""
    app = _ctx(ctx)
    try:
        # Parallel: get project, tasks, and notes
        project_fut = app.client.get_page(project_id)
        tasks_fut = app.client.query_all(
            app.config.tasks_ds_id,
            filter={"property": "Project", "relation": {"contains": project_id}},
        )
        notes_fut = app.client.query_all(
            app.config.notes_ds_id,
            filter={"property": "Project", "relation": {"contains": project_id}},
            sorts=[{"property": "Note Date", "direction": "descending"}],
        )
        project_page, task_pages, note_pages = await asyncio.gather(
            project_fut, tasks_fut, notes_fut
        )

        project = format_project(project_page)
        if resolve_relations:
            await _resolve_truncated_relations(app, project_page, project)
        loc_name = app.tasks_schema.location_property_name
        tasks = [format_task(t, location_property_name=loc_name) for t in task_pages]
        notes = [format_note(n) for n in note_pages[:10]]

        # Task breakdown by status
        breakdown: dict[str, list[dict]] = {}
        for t in tasks:
            s = t.get("status", "Unknown")
            breakdown.setdefault(s, []).append(t)

        project["tasks"] = {
            "total": len(tasks),
            "by_status": {s: len(ts) for s, ts in breakdown.items()},
            "items": tasks,
        }
        project["recent_notes"] = notes
        return project
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_projects to find valid project IDs.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def list_project_templates(ctx: Context = None) -> dict:
    """List page templates defined on the Projects database (name, id, whether it's the
    default). Use the returned IDs with create_project's template_id param, or pass
    use_default_template=true on create_project if one is marked default."""
    app = _ctx(ctx)
    try:
        templates = await app.client.list_templates(app.config.projects_ds_id)
        return {
            "templates": [
                {"id": t.get("id"), "name": t.get("name"), "is_default": t.get("is_default", False)}
                for t in templates
            ]
        }
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def create_project(
    name: Annotated[str, Field(description="Project name.")],
    status: Annotated[
        str | None,
        Field(
            description=f"Status. Options: {', '.join(PROJECT_STATUSES)}. Defaults to Not Started."
        ),
    ] = None,
    deadline: Annotated[str | None, Field(description="Deadline in YYYY-MM-DD format.")] = None,
    tag_id: Annotated[str | None, Field(description="Tag page ID to link.")] = None,
    goal_id: Annotated[str | None, Field(description="Goal page ID to link.")] = None,
    content: Annotated[
        str | None,
        Field(
            description=(
                "Page body content as markdown. Supports: # headings, - bullets, "
                "1. numbered lists, - [ ] to-dos, ```code blocks```, > quotes, --- dividers, "
                "and plain paragraphs. Mutually exclusive with use_default_template/template_id."
            )
        ),
    ] = None,
    use_default_template: Annotated[
        bool,
        Field(
            description=(
                "Apply the Projects database's default page template (if one is set), "
                "same as clicking 'New' in Notion. Mutually exclusive with content/template_id. "
                "Use list_project_templates to see if a default exists and find named templates."
            )
        ),
    ] = False,
    template_id: Annotated[
        str | None,
        Field(
            description=(
                "Apply a specific named template by ID instead of the default. "
                "Use list_project_templates to find IDs. Mutually exclusive with content."
            )
        ),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Create a new project. Use search_tags to find tag IDs, search_goals for goal IDs.
    Use list_project_templates + use_default_template/template_id to spin up a project
    from an existing Notion template (e.g. one with pre-defined tasks) instead of blank."""
    app = _ctx(ctx)
    props: dict = {"Name": _prop_title(name)}
    if status:
        props["Status"] = _prop_status(status)
    if deadline:
        props["Target Deadline"] = _prop_date(deadline)
    if tag_id:
        props["Tag"] = _prop_relation([tag_id])
    if goal_id:
        props["Goal"] = _prop_relation([goal_id])

    if content and (use_default_template or template_id):
        return _error(
            "content is mutually exclusive with use_default_template/template_id "
            "— Notion doesn't allow setting page body content on a templated create."
        )

    children = text_to_blocks(content) if content else None
    template: dict | None = None
    if template_id:
        template = {"type": "template_id", "template_id": template_id}
    elif use_default_template:
        template = {"type": "default"}

    try:
        page = await app.client.create_page(
            app.config.projects_ds_id, props, children=children, template=template
        )
        return format_project(page)
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def update_project(
    project_id: Annotated[str, Field(description="Project page ID to update.")],
    name: Annotated[str | None, Field(description="New project name.")] = None,
    status: Annotated[
        str | None,
        Field(description=f"New status. Options: {', '.join(PROJECT_STATUSES)}."),
    ] = None,
    deadline: Annotated[str | None, Field(description="New deadline (YYYY-MM-DD).")] = None,
    tag_id: Annotated[str | None, Field(description="New tag page ID.")] = None,
    goal_id: Annotated[str | None, Field(description="New goal page ID.")] = None,
    ctx: Context = None,
) -> dict:
    """Update project properties. Only provided fields are changed.
    Auto-sets Completed date when status is changed to Done."""
    app = _ctx(ctx)
    props: dict = {}
    if name is not None:
        props["Name"] = _prop_title(name)
    if status is not None:
        props["Status"] = _prop_status(status)
        if status == "Done":
            props["Completed"] = _prop_date(_today())
    if deadline is not None:
        props["Target Deadline"] = _prop_date(deadline)
    if tag_id is not None:
        props["Tag"] = _prop_relation([tag_id])
    if goal_id is not None:
        props["Goal"] = _prop_relation([goal_id])

    if not props:
        return _error("No properties to update.")

    try:
        page = await app.client.update_page(project_id, props)
        return format_project(page)
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_projects to find valid project IDs.")


# =========================================================================
#  NOTES (4 tools)
# =========================================================================


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def search_notes(
    note_type: Annotated[
        str | None,
        Field(description=f"Filter by type. Options: {', '.join(NOTE_TYPES)}."),
    ] = None,
    project_id: Annotated[str | None, Field(description="Filter by project page ID.")] = None,
    tag_id: Annotated[str | None, Field(description="Filter by tag page ID.")] = None,
    favorite: Annotated[bool | None, Field(description="Filter to favorites only.")] = None,
    date_after: Annotated[
        str | None, Field(description="Notes on or after this date (YYYY-MM-DD).")
    ] = None,
    query: Annotated[str | None, Field(description="Text to search for in note titles.")] = None,
    limit: Annotated[int, Field(description="Maximum results.", ge=1, le=100)] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """Search notes by title text, type, project, tag, favorite status, or date.
    For note content/body, use get_note_content with the note ID."""
    app = _ctx(ctx)
    if (err := _validate_note_type(app, note_type)) is not None:
        return err
    filters: list[dict] = []

    if note_type:
        filters.append({"property": "Type", "select": {"equals": note_type}})
    if project_id:
        filters.append({"property": "Project", "relation": {"contains": project_id}})
    if tag_id:
        filters.append({"property": "Tag", "relation": {"contains": tag_id}})
    if favorite is True:
        filters.append({"property": "Favorite", "checkbox": {"equals": True}})
    if date_after:
        filters.append({"property": "Note Date", "date": {"on_or_after": date_after}})
    if query:
        filters.append({"property": "Name", "title": {"contains": query}})

    query_filter = {"and": filters} if len(filters) > 1 else (filters[0] if filters else None)
    sorts = [{"property": "Note Date", "direction": "descending"}]

    try:
        pages = await app.client.query_all(app.config.notes_ds_id, filter=query_filter, sorts=sorts)
        return [format_note(p) for p in pages[:limit]]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_note_content(
    note_id: Annotated[str, Field(description="Note page ID.")],
    ctx: Context = None,
) -> dict:
    """Get note properties plus the full page body as enhanced Markdown.
    Use search_notes to find note IDs."""
    app = _ctx(ctx)
    try:
        page, content = await asyncio.gather(
            app.client.get_page(note_id),
            _read_page_markdown(app, note_id),
        )
        result = format_note(page)
        result["content"] = content
        return result
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_notes to find valid note IDs.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def create_note(
    name: Annotated[str, Field(description="Note title.")],
    note_type: Annotated[
        str | None,
        Field(description=f"Note type. Options: {', '.join(NOTE_TYPES)}."),
    ] = None,
    project_id: Annotated[str | None, Field(description="Project page ID to link.")] = None,
    tag_ids: Annotated[list[str] | None, Field(description="Tag page IDs to link.")] = None,
    source_url: Annotated[str | None, Field(description="Source URL for the note.")] = None,
    content: Annotated[
        str | None,
        Field(
            description=(
                "Page body content as markdown. Supports: # headings, - bullets, "
                "1. numbered lists, - [ ] to-dos, ```code blocks```, > quotes, --- dividers, "
                "and plain paragraphs."
            )
        ),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Create a new note. Use search_projects for project IDs, search_tags for tag IDs."""
    app = _ctx(ctx)
    if (err := _validate_note_type(app, note_type)) is not None:
        return err
    props: dict = {
        "Name": _prop_title(name),
        "Note Date": _prop_date(_today()),
    }
    if note_type:
        props["Type"] = _prop_select(note_type)
    if project_id:
        props["Project"] = _prop_relation([project_id])
    if tag_ids:
        props["Tag"] = _prop_relation(tag_ids)
    if source_url:
        props["URL"] = _prop_url(source_url)

    children = text_to_blocks(content) if content else None
    possible_duplicate = await _find_possible_duplicate(
        app, app.config.notes_ds_id, name, format_note
    )

    try:
        page = await app.client.create_page(app.config.notes_ds_id, props, children=children)
        result = format_note(page)
        if possible_duplicate:
            result["possible_duplicate"] = possible_duplicate
        return result
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def update_note(
    note_id: Annotated[str, Field(description="Note page ID to update.")],
    name: Annotated[str | None, Field(description="New note title.")] = None,
    note_type: Annotated[
        str | None,
        Field(description=f"New type. Options: {', '.join(NOTE_TYPES)}."),
    ] = None,
    project_id: Annotated[str | None, Field(description="New project page ID.")] = None,
    tag_ids: Annotated[
        list[str] | None, Field(description="New tag page IDs (replaces existing).")
    ] = None,
    favorite: Annotated[bool | None, Field(description="Set favorite flag.")] = None,
    source_url: Annotated[str | None, Field(description="New source URL.")] = None,
    ctx: Context = None,
) -> dict:
    """Update note properties. Only provided fields are changed."""
    app = _ctx(ctx)
    if (err := _validate_note_type(app, note_type)) is not None:
        return err
    props: dict = {}
    if name is not None:
        props["Name"] = _prop_title(name)
    if note_type is not None:
        props["Type"] = _prop_select(note_type)
    if project_id is not None:
        props["Project"] = _prop_relation([project_id])
    if tag_ids is not None:
        props["Tag"] = _prop_relation(tag_ids)
    if favorite is not None:
        props["Favorite"] = _prop_checkbox(favorite)
    if source_url is not None:
        props["URL"] = _prop_url(source_url)

    if not props:
        return _error("No properties to update.")

    try:
        page = await app.client.update_page(note_id, props)
        return format_note(page)
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_notes to find valid note IDs.")


# =========================================================================
#  TAGS (3 tools)
# =========================================================================


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def search_tags(
    tag_type: Annotated[
        str | None,
        Field(description=f"Filter by PARA type. Options: {', '.join(TAG_TYPES)}."),
    ] = None,
    query: Annotated[
        str | None,
        Field(description="Text to search for in tag names."),
    ] = None,
    parent_tag_id: Annotated[
        str | None,
        Field(description="Filter by parent tag page ID."),
    ] = None,
    favorite: Annotated[
        bool | None,
        Field(description="Filter by favorite flag."),
    ] = None,
    limit: Annotated[int, Field(description="Maximum results.", ge=1, le=100)] = 100,
    ctx: Context = None,
) -> list[dict] | dict:
    """Search tags by name, PARA type, parent tag, or favorite flag.
    Tags organize content across all databases in Ultimate Brain."""
    app = _ctx(ctx)
    filters: list[dict] = []

    if tag_type:
        filters.append({"property": "Type", "status": {"equals": tag_type}})
    if query:
        filters.append({"property": "Name", "title": {"contains": query}})
    if parent_tag_id:
        filters.append({"property": "Parent Tag", "relation": {"contains": parent_tag_id}})
    if favorite is not None:
        filters.append({"property": "Favorite", "checkbox": {"equals": favorite}})

    query_filter = {"and": filters} if len(filters) > 1 else (filters[0] if filters else None)

    try:
        pages = await app.client.query_all(app.config.tags_ds_id, filter=query_filter)
        return [format_tag(p) for p in pages[:limit]]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def create_tag(
    name: Annotated[str, Field(description="Tag name.")],
    tag_type: Annotated[
        str | None,
        Field(description=f"PARA type. Options: {', '.join(TAG_TYPES)}."),
    ] = None,
    parent_tag_id: Annotated[str | None, Field(description="Parent tag page ID.")] = None,
    ctx: Context = None,
) -> dict:
    """Create a new tag. Tags use the PARA methodology: Area (responsibility), Resource (topic), Entity (person/place)."""
    app = _ctx(ctx)
    props: dict = {"Name": _prop_title(name)}
    if tag_type:
        props["Type"] = _prop_status(tag_type)
    if parent_tag_id:
        props["Parent Tag"] = _prop_relation([parent_tag_id])

    try:
        page = await app.client.create_page(app.config.tags_ds_id, props)
        return format_tag(page)
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def update_tag(
    tag_id: Annotated[str, Field(description="Tag page ID to update.")],
    name: Annotated[str | None, Field(description="New tag name.")] = None,
    tag_type: Annotated[
        str | None,
        Field(description=f"New PARA type. Options: {', '.join(TAG_TYPES)}."),
    ] = None,
    parent_tag_id: Annotated[str | None, Field(description="New parent tag page ID.")] = None,
    favorite: Annotated[bool | None, Field(description="Set favorite flag.")] = None,
    ctx: Context = None,
) -> dict:
    """Update tag properties. Only provided fields are changed."""
    app = _ctx(ctx)
    props: dict = {}
    if name is not None:
        props["Name"] = _prop_title(name)
    if tag_type is not None:
        props["Type"] = _prop_status(tag_type)
    if parent_tag_id is not None:
        props["Parent Tag"] = _prop_relation([parent_tag_id])
    if favorite is not None:
        props["Favorite"] = _prop_checkbox(favorite)

    if not props:
        return _error("No properties to update.")

    try:
        page = await app.client.update_page(tag_id, props)
        return format_tag(page)
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_tags to find valid tag IDs.")


# =========================================================================
#  GOALS (4 tools)
# =========================================================================


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def search_goals(
    status: Annotated[
        str | None,
        Field(
            description=f"Filter by status. Options: {', '.join(GOAL_STATUSES)}. Defaults to Active."
        ),
    ] = None,
    query: Annotated[
        str | None,
        Field(description="Text to search for in goal names."),
    ] = None,
    tag_id: Annotated[
        str | None,
        Field(description="Filter by tag page ID."),
    ] = None,
    project_id: Annotated[
        str | None,
        Field(description="Filter by linked project page ID."),
    ] = None,
    deadline_before: Annotated[
        str | None,
        Field(description="Target deadline on or before this date (YYYY-MM-DD)."),
    ] = None,
    deadline_after: Annotated[
        str | None,
        Field(description="Target deadline on or after this date (YYYY-MM-DD)."),
    ] = None,
    achieved_before: Annotated[
        str | None,
        Field(
            description="Achieved date on or before this date (YYYY-MM-DD). Best combined with status='Achieved'."
        ),
    ] = None,
    achieved_after: Annotated[
        str | None,
        Field(
            description="Achieved date on or after this date (YYYY-MM-DD). Best combined with status='Achieved'."
        ),
    ] = None,
    limit: Annotated[int, Field(description="Maximum results.", ge=1, le=100)] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """Search goals by name, status, tag, project, deadline, or achieved date.
    Defaults to Active goals. Combine deadline_before + deadline_after for date ranges.
    For goal details with linked projects, use get_goal_detail."""
    app = _ctx(ctx)
    filters: list[dict] = []

    if status:
        filters.append({"property": "Status", "status": {"equals": status}})
    else:
        filters.append({"property": "Status", "status": {"equals": "Active"}})

    if query:
        filters.append({"property": "Name", "title": {"contains": query}})
    if tag_id:
        filters.append({"property": "Tag", "relation": {"contains": tag_id}})
    if project_id:
        filters.append({"property": "Projects", "relation": {"contains": project_id}})
    if deadline_before:
        filters.append({"property": "Target Deadline", "date": {"on_or_before": deadline_before}})
    if deadline_after:
        filters.append({"property": "Target Deadline", "date": {"on_or_after": deadline_after}})
    if achieved_before:
        filters.append({"property": "Achieved", "date": {"on_or_before": achieved_before}})
    if achieved_after:
        filters.append({"property": "Achieved", "date": {"on_or_after": achieved_after}})

    query_filter = {"and": filters} if len(filters) > 1 else (filters[0] if filters else None)
    sorts = [{"property": "Target Deadline", "direction": "ascending"}]

    try:
        pages = await app.client.query_all(app.config.goals_ds_id, filter=query_filter, sorts=sorts)
        return [format_goal(p) for p in pages[:limit]]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_goal_detail(
    goal_id: Annotated[str, Field(description="Goal page ID.")],
    resolve_relations: Annotated[
        bool,
        Field(
            description=(
                "If any relation on the goal is truncated at Notion's 25-item inline "
                "cap (see _truncated_relations), fetch the full list via an extra "
                "paginated API call. Note: the 'projects' field below is always fully "
                "resolved via a separate live query regardless of this flag — this "
                "only affects the raw project_ids/tag_ids fields."
            )
        ),
    ] = False,
    ctx: Context = None,
) -> dict:
    """Get goal properties plus all linked projects with their status and progress.
    Use search_goals to find goal IDs."""
    app = _ctx(ctx)
    try:
        page = await app.client.get_page(goal_id)
        goal = format_goal(page)
        if resolve_relations:
            await _resolve_truncated_relations(app, page, goal)

        # Get linked projects
        project_ids = goal.get("project_ids", [])
        if project_ids:
            # Query projects linked to this goal
            projects_pages = await app.client.query_all(
                app.config.projects_ds_id,
                filter={"property": "Goal", "relation": {"contains": goal_id}},
            )
            goal["projects"] = [format_project(p) for p in projects_pages]
        else:
            goal["projects"] = []

        return goal
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_goals to find valid goal IDs.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def create_goal(
    name: Annotated[str, Field(description="Goal name.")],
    status: Annotated[
        str | None,
        Field(description=f"Status. Options: {', '.join(GOAL_STATUSES)}. Defaults to Active."),
    ] = None,
    deadline: Annotated[str | None, Field(description="Deadline in YYYY-MM-DD format.")] = None,
    tag_id: Annotated[str | None, Field(description="Tag page ID to link.")] = None,
    project_ids: Annotated[list[str] | None, Field(description="Project page IDs to link.")] = None,
    content: Annotated[
        str | None,
        Field(
            description=(
                "Page body content as markdown. Supports: # headings, - bullets, "
                "1. numbered lists, - [ ] to-dos, ```code blocks```, > quotes, --- dividers, "
                "and plain paragraphs."
            )
        ),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Create a new goal. Use search_tags for tag IDs, search_projects for project IDs."""
    app = _ctx(ctx)
    props: dict = {"Name": _prop_title(name)}
    if status:
        props["Status"] = _prop_status(status)
    if deadline:
        props["Target Deadline"] = _prop_date(deadline)
    if tag_id:
        props["Tag"] = _prop_relation([tag_id])
    if project_ids:
        props["Projects"] = _prop_relation(project_ids)

    children = text_to_blocks(content) if content else None

    try:
        page = await app.client.create_page(app.config.goals_ds_id, props, children=children)
        return format_goal(page)
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def update_goal(
    goal_id: Annotated[str, Field(description="Goal page ID to update.")],
    name: Annotated[str | None, Field(description="New goal name.")] = None,
    status: Annotated[
        str | None,
        Field(description=f"New status. Options: {', '.join(GOAL_STATUSES)}."),
    ] = None,
    deadline: Annotated[str | None, Field(description="New deadline (YYYY-MM-DD).")] = None,
    tag_id: Annotated[str | None, Field(description="New tag page ID.")] = None,
    project_ids: Annotated[
        list[str] | None, Field(description="New project page IDs (replaces existing).")
    ] = None,
    ctx: Context = None,
) -> dict:
    """Update goal properties. Only provided fields are changed.
    Auto-sets Achieved date when status is changed to Achieved."""
    app = _ctx(ctx)
    props: dict = {}
    if name is not None:
        props["Name"] = _prop_title(name)
    if status is not None:
        props["Status"] = _prop_status(status)
        if status == "Achieved":
            props["Achieved"] = _prop_date(_today())
    if deadline is not None:
        props["Target Deadline"] = _prop_date(deadline)
    if tag_id is not None:
        props["Tag"] = _prop_relation([tag_id])
    if project_ids is not None:
        props["Projects"] = _prop_relation(project_ids)

    if not props:
        return _error("No properties to update.")

    try:
        page = await app.client.update_page(goal_id, props)
        return format_goal(page)
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_goals to find valid goal IDs.")


# =========================================================================
#  CROSS-CUTTING (3 tools)
# =========================================================================


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def daily_summary(
    ctx: Context = None,
) -> dict:
    """Get a complete daily overview in a single call: My Day tasks, overdue tasks,
    inbox count, active projects count, and active goals count.
    Start here for a quick status check."""
    app = _ctx(ctx)
    today = _today()

    try:
        # 5 parallel queries
        my_day_fut = app.client.query_all(
            app.config.tasks_ds_id,
            filter={
                "and": [
                    {"property": "My Day", "checkbox": {"equals": True}},
                    {"property": "Status", "status": {"does_not_equal": "Done"}},
                ]
            },
        )
        overdue_fut = app.client.query_all(
            app.config.tasks_ds_id,
            filter={
                "and": [
                    {"property": "Due", "date": {"before": today}},
                    {"property": "Status", "status": {"does_not_equal": "Done"}},
                ]
            },
        )
        inbox_fut = app.client.query_all(
            app.config.tasks_ds_id,
            filter={
                "and": [
                    {"property": "Status", "status": {"equals": "To Do"}},
                    {"property": "Project", "relation": {"is_empty": True}},
                    {"property": "Due", "date": {"is_empty": True}},
                ]
            },
        )
        projects_fut = app.client.query_all(
            app.config.projects_ds_id,
            filter={
                "or": [
                    {"property": "Status", "status": {"equals": "Doing"}},
                    {"property": "Status", "status": {"equals": "Ongoing"}},
                ]
            },
        )
        goals_fut = app.client.query_all(
            app.config.goals_ds_id,
            filter={"property": "Status", "status": {"equals": "Active"}},
        )

        my_day, overdue, inbox, projects, goals = await asyncio.gather(
            my_day_fut, overdue_fut, inbox_fut, projects_fut, goals_fut
        )

        loc_name = app.tasks_schema.location_property_name
        my_day_tasks = [format_task(p, location_property_name=loc_name) for p in my_day]
        priority_order = {"High": 0, "Medium": 1, "Low": 2, None: 3}
        my_day_tasks.sort(key=lambda t: priority_order.get(t.get("priority"), 3))

        return {
            "date": today,
            "my_day": {
                "count": len(my_day_tasks),
                "tasks": my_day_tasks,
            },
            "overdue": {
                "count": len(overdue),
                "tasks": [format_task(p, location_property_name=loc_name) for p in overdue],
            },
            "inbox_count": len(inbox),
            "active_projects_count": len(projects),
            "active_goals_count": len(goals),
        }
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=True, idempotentHint=True)
)
async def archive_item(
    page_id: Annotated[
        str,
        Field(
            description="Page ID of any Ultimate Brain item (task, project, note, tag, or goal)."
        ),
    ],
    ctx: Context = None,
) -> dict:
    """Archive any Ultimate Brain item by setting its Archived checkbox to true.
    This is the 'delete' operation in UB — items remain in the database but are hidden
    from all dashboards and views. This action can be reversed by unchecking Archived."""
    app = _ctx(ctx)
    try:
        page = await app.client.update_page(page_id, {"Archived": _prop_checkbox(True)})
        return {"archived": True, "id": page.get("id"), "url": page.get("url", "")}
    except NotionAPIError as e:
        return _handle_api_error(e, "Check the page ID is valid.")


def _markdown_unsupported(err: NotionAPIError) -> bool:
    """True if the error means the markdown endpoint/version is unavailable.

    The page-markdown endpoints require API version 2026-03-11; on a workspace
    that does not have it the route returns 400/404. We only fall back to the
    legacy block path for those — auth (401/403), rate-limit/server (429/5xx,
    already retried) and other errors propagate instead of being silently
    masked by a different engine.
    """
    return err.status in (400, 404)


async def _read_blocks_as_text(app: AppContext, page_id: str) -> str:
    blocks = await app.client.get_blocks(page_id, recursive=True)
    return blocks_to_text(blocks)


async def _read_page_markdown(app: AppContext, page_id: str) -> str:
    """Return a page body as enhanced Markdown via Notion's server-side endpoint.

    Falls back to the local block-to-text converter only when the markdown
    endpoint is genuinely unavailable on the workspace — detected on the first
    version-unavailable error and remembered via ``app.markdown_supported``.
    Once support is confirmed, errors surface instead of silently degrading.
    Appends a notice when Notion reports truncation or unrendered blocks.
    """
    if app.markdown_supported is False:
        return await _read_blocks_as_text(app, page_id)
    try:
        data = await app.client.get_page_markdown(page_id)
    except NotionAPIError as err:
        # Only treat as "endpoint absent" if we have not already confirmed
        # support; otherwise this is a real error and must propagate.
        if app.markdown_supported is None and _markdown_unsupported(err):
            app.markdown_supported = False
            return await _read_blocks_as_text(app, page_id)
        raise
    app.markdown_supported = True
    markdown = data.get("markdown", "") or ""
    if data.get("truncated"):
        markdown += "\n\n_[content truncated by Notion — page exceeds the block limit]_"
    unknown = data.get("unknown_block_ids") or []
    if unknown:
        markdown += (
            f"\n\n_[{len(unknown)} block(s) could not be rendered as Markdown "
            f"and are omitted: {', '.join(unknown)}]_"
        )
    return markdown


@mcp.tool(
    # idempotent only in 'replace' mode; 'append' adds content each call.
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=True, idempotentHint=False)
)
async def set_page_content(
    page_id: Annotated[str, Field(description="Page ID of any Notion page.")],
    content: Annotated[
        str,
        Field(
            description=(
                "Page body content as enhanced Markdown. Supports # headings, "
                "- bullets, 1. numbered lists, - [ ] to-dos, ```code blocks```, "
                "> quotes, --- dividers, tables, and plain paragraphs. NOTE: rich "
                "blocks (tables, toggles, deep nesting) only round-trip faithfully "
                "in 'replace' mode; 'append' uses a simpler local converter."
            )
        ),
    ],
    mode: Annotated[
        Literal["replace", "append"],
        Field(
            description="'replace' overwrites the whole page body (default). 'append' adds after existing content."
        ),
    ] = "replace",
    ctx: Context = None,
) -> dict:
    """Set or update the body content of any page. Use 'replace' mode to overwrite
    existing content, or 'append' to add below it. Pass empty content with 'replace'
    to clear the page body. Works on any page type (tasks, projects, notes, goals, etc.).

    'replace' uses Notion's server-side Markdown endpoint (handles tables, toggles,
    nesting, and block-splitting natively). 'append' uses the local block builder,
    which only supports a subset of block types (no tables/toggles)."""
    app = _ctx(ctx)

    async def _replace_via_blocks() -> dict:
        new_blocks = text_to_blocks(content)
        existing = await app.client.get_blocks(page_id)
        deleted = 0
        if existing:
            await _bounded_gather([app.client.delete_block(b["id"]) for b in existing])
            deleted = len(existing)
        if new_blocks:
            await app.client.append_blocks(page_id, new_blocks)
        return {
            "ok": True,
            "page_id": page_id,
            "mode": "replace",
            "engine": "blocks",
            "blocks_written": len(new_blocks),
            "blocks_deleted": deleted,
        }

    try:
        if mode == "replace":
            # Known-unavailable workspace → block path directly.
            if app.markdown_supported is False:
                return await _replace_via_blocks()
            try:
                await app.client.replace_page_markdown(
                    page_id, content, allow_deleting_content=True
                )
                app.markdown_supported = True
                return {
                    "ok": True,
                    "page_id": page_id,
                    "mode": mode,
                    "engine": "markdown",
                    "blocks_written": None,
                    "blocks_deleted": None,
                }
            except NotionAPIError as err:
                # Only fall back if we have NOT confirmed markdown support and the
                # error looks like the endpoint is absent. Once support is known,
                # a 400 means a real content error and must surface — never write
                # silently-degraded content while reporting success.
                if app.markdown_supported is None and _markdown_unsupported(err):
                    app.markdown_supported = False
                    return await _replace_via_blocks()
                raise

        # append mode — block builder
        new_blocks = text_to_blocks(content)
        if new_blocks:
            await app.client.append_blocks(page_id, new_blocks)
        return {
            "ok": True,
            "page_id": page_id,
            "mode": mode,
            "engine": "blocks",
            "blocks_written": len(new_blocks),
            "blocks_deleted": 0,
        }
    except NotionAPIError as e:
        return _handle_api_error(e, "Check the page ID is valid.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=True, idempotentHint=False)
)
async def patch_page_content(
    page_id: Annotated[str, Field(description="Page ID of any Notion page.")],
    edits: Annotated[
        list[dict],
        Field(
            description=(
                "Up to 100 find-and-replace edits applied in order. Each is "
                "{'old_str': <exact existing Markdown>, 'new_str': <replacement, "
                "must be non-empty>, 'replace_all_matches': <bool, optional, default "
                "false>}. old_str must match the page's current Markdown exactly (use "
                "get_page_content to see it). To delete content, use set_page_content."
            )
        ),
    ],
    ctx: Context = None,
) -> dict:
    """Apply targeted find-and-replace edits to a page's body without rewriting
    the whole page. Far cheaper than set_page_content for small changes (fix a
    line, check off a to-do, update a value). Uses Notion's server-side Markdown
    edit endpoint; old_str must match the current Markdown exactly.

    Edits are applied one at a time, in order, against the document AS MUTATED by
    the preceding edits (so overlapping edits compound). Each is reported
    precisely: the result gives `edits_applied` and an `unmatched` list for edits
    that found no match (skipped, not fatal — the rest still apply). If a hard
    error occurs partway, the error payload still reports how far it got
    (`edits_applied`/`unmatched` so far) since earlier edits already mutated the
    page. Requires Notion API 2026-03-11 (no block fallback for this tool)."""
    app = _ctx(ctx)
    if not edits:
        return _error("Provide at least one edit ({'old_str', 'new_str'}).")
    if len(edits) > 100:
        return _error("At most 100 edits per call.")
    for i, e in enumerate(edits):
        if not isinstance(e, dict):
            return _error(f"edits[{i}] must be an object with 'old_str' and 'new_str'.")
        if "old_str" not in e or "new_str" not in e:
            return _error(f"edits[{i}] must have both 'old_str' and 'new_str'.")
        if not isinstance(e["old_str"], str) or not isinstance(e["new_str"], str):
            return _error(f"edits[{i}]: 'old_str' and 'new_str' must be strings.")
        if not e["new_str"]:
            return _error(
                f"edits[{i}]: 'new_str' must be non-empty (deletion is not "
                "supported here — use set_page_content to remove content)."
            )
        if "replace_all_matches" in e and not isinstance(e["replace_all_matches"], bool):
            return _error(f"edits[{i}]: 'replace_all_matches' must be a boolean.")

    # Apply individually: Notion 400s when a single edit matches nothing, but a
    # multi-edit batch silently skips non-matching edits and still returns 200.
    # Looping lets us report exactly which edits landed and which found no match.
    # The "no matches found" detection relies on Notion's 400 message text (there
    # is no machine-readable code distinguishing it from other validation_errors);
    # any other error surfaces with the progress so far attached.
    applied = 0
    unmatched: list[dict] = []
    for i, e in enumerate(edits):
        try:
            await app.client.update_page_markdown(page_id, [e])
            applied += 1
        except NotionAPIError as err:
            if err.status == 400 and "no matches found" in str(err).lower():
                unmatched.append({"index": i, "old_str": e["old_str"]})
                continue
            result = _handle_api_error(
                err,
                "The find-and-replace endpoint requires Notion API 2026-03-11. "
                "If the page exists, check old_str matches the current Markdown "
                "exactly — call get_page_content to see it.",
            )
            # Surface how far we got: edits before index i already mutated the page.
            result["edits_applied"] = applied
            result["unmatched"] = unmatched
            result["failed_at_index"] = i
            return result

    return {
        "ok": not unmatched,
        "page_id": page_id,
        "edits_applied": applied,
        "unmatched": unmatched,
    }


# =========================================================================
#  WORKFLOW CONSOLIDATORS (2 tools)
# =========================================================================


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def daily_review_snapshot(
    inbox_limit: Annotated[
        int,
        Field(description="Maximum inbox tasks to return.", ge=1, le=200),
    ] = _SNAPSHOT_BUCKET_CAP,
    ctx: Context = None,
) -> dict:
    """Get a complete daily-review snapshot in one call: current time, all five
    task buckets needed for review, the deduplicated outstanding set, project and
    area-tag lookup tables, and the Tasks data source schema for location handling.

    Use this at the START of a daily review — replaces 7 separate read calls
    (search_tasks ×4, get_inbox_tasks, search_projects, search_tags) plus the
    get_page sample needed to discover the Location property.

    Returns:
      now              — ISO8601 with offset, e.g. '2026-05-09T17:21:33+01:00'
      timezone         — IANA name, e.g. 'Europe/London'
      buckets:
        completed_today        — tasks marked Done with completion_date = today
        overdue_or_due_today   — non-Done tasks with due ≤ today
        due_tomorrow           — non-Done tasks due exactly tomorrow
        on_my_day              — non-Done tasks with My Day flag set (any due date)
        inbox                  — non-Done tasks with status To Do, no project, no due
        routine_blocks         — non-Done tasks with Recur Unit set and Due in
                                 [today, today + 7 days]. Each carries its
                                 own due/due_end (time block) plus, when present,
                                 the parent's already-planned sub-tasks for the
                                 same cycle. Backs the per-block planning model
                                 in docs/hermes/daily-plan-phase.md.
      outstanding      — deduplicated union of overdue_or_due_today ∪ on_my_day
      lookups:
        projects       — {id → {name, status}} for active projects (Doing + Ongoing)
        area_tags      — {id → {name}} for tags with type=Area
        routine_blocks — parent_id → {name, due, due_end, planned_subtasks: [...]}
                        — same id as the buckets.routine_blocks parent task.
                        Lets the planning pass call back with sub-task ids without
                        resolving them from the page id alone.
      task_schema:
        has_location_property      — bool
        location_property_name     — string or null
        location_property_type     — 'select' | 'multi_select' | 'status' | null
        location_options           — string[] of valid values
        labels_options             — string[] of valid Labels multi_select values
      truncated        — {bucket_name → bool} flagging buckets that hit their cap

    For just counts (no per-task details), use daily_summary instead — much smaller
    response. For a single task's full content, use get_page or get_page_content.
    """
    app = _ctx(ctx)
    tz = ZoneInfo(app.config.timezone)
    now_dt = datetime.now(tz)
    today = now_dt.date().isoformat()
    tomorrow = (now_dt.date() + timedelta(days=1)).isoformat()

    # Build filters once
    not_done = {"property": "Status", "status": {"does_not_equal": "Done"}}
    completed_today_filter = {
        "and": [
            {"property": "Status", "status": {"equals": "Done"}},
            {"property": "Completed", "date": {"on_or_after": today}},
            {"property": "Completed", "date": {"on_or_before": today}},
        ]
    }
    overdue_or_today_filter = {
        "and": [not_done, {"property": "Due", "date": {"on_or_before": today}}]
    }
    due_tomorrow_filter = {"and": [not_done, {"property": "Due", "date": {"equals": tomorrow}}]}
    on_my_day_filter = {"and": [not_done, {"property": "My Day", "checkbox": {"equals": True}}]}
    inbox_filter = {
        "and": [
            {"property": "Status", "status": {"equals": "To Do"}},
            {"property": "Project", "relation": {"is_empty": True}},
            {"property": "Due", "date": {"is_empty": True}},
        ]
    }
    active_projects_filter = {
        "or": [
            {"property": "Status", "status": {"equals": "Doing"}},
            {"property": "Status", "status": {"equals": "Ongoing"}},
        ]
    }
    area_tags_filter = {"property": "Type", "status": {"equals": "Area"}}

    # Routine blocks: recurring tasks with a Next Due in the next week. The
    # date window is wide enough to capture weekly/monthly routines that land
    # within planning horizon without hauling in far-future instances; the
    # actual planning pass filters further by exact day.
    horizon_end = (now_dt.date() + timedelta(days=7)).isoformat()
    routine_blocks_filter = {
        "and": [
            not_done,
            {"property": "Recur Unit", "select": {"is_not_empty": True}},
            {"property": "Due", "date": {"on_or_after": today}},
            {"property": "Due", "date": {"on_or_before": horizon_end}},
        ]
    }

    try:
        (
            completed_pages,
            overdue_pages,
            tomorrow_pages,
            my_day_pages,
            inbox_pages,
            project_pages,
            area_tag_pages,
            routine_pages,
        ) = await asyncio.gather(
            app.client.query_all(app.config.tasks_ds_id, filter=completed_today_filter),
            app.client.query_all(app.config.tasks_ds_id, filter=overdue_or_today_filter),
            app.client.query_all(app.config.tasks_ds_id, filter=due_tomorrow_filter),
            app.client.query_all(app.config.tasks_ds_id, filter=on_my_day_filter),
            app.client.query_all(app.config.tasks_ds_id, filter=inbox_filter),
            app.client.query_all(app.config.projects_ds_id, filter=active_projects_filter),
            app.client.query_all(app.config.tags_ds_id, filter=area_tags_filter),
            app.client.query_all(app.config.tasks_ds_id, filter=routine_blocks_filter),
        )
    except NotionAPIError as e:
        return _handle_api_error(e)

    # Build lookups so format_task can resolve names
    project_lookup: dict[str, dict] = {}
    for p in project_pages:
        formatted = format_project(p)
        project_lookup[formatted["id"]] = {
            "name": formatted.get("name", ""),
            "status": formatted.get("status"),
        }
    tag_lookup: dict[str, dict] = {}
    for t in area_tag_pages:
        formatted = format_tag(t)
        tag_lookup[formatted["id"]] = {"name": formatted.get("name", "")}

    loc_name = app.tasks_schema.location_property_name

    def _fmt_bucket(pages: list[dict], cap: int) -> tuple[list[dict], bool]:
        truncated = len(pages) > cap
        sliced = pages[:cap]
        return (
            [
                format_task(
                    p,
                    project_lookup=project_lookup,
                    tag_lookup=tag_lookup,
                    location_property_name=loc_name,
                )
                for p in sliced
            ],
            truncated,
        )

    bucket_cap = _SNAPSHOT_BUCKET_CAP
    completed_today, t_completed = _fmt_bucket(completed_pages, bucket_cap)
    overdue_or_due_today, t_overdue = _fmt_bucket(overdue_pages, bucket_cap)
    due_tomorrow, t_tomorrow = _fmt_bucket(tomorrow_pages, bucket_cap)
    on_my_day, t_my_day = _fmt_bucket(my_day_pages, bucket_cap)
    inbox, t_inbox = _fmt_bucket(inbox_pages, inbox_limit)
    routine_blocks, t_routine = _fmt_bucket(routine_pages, bucket_cap)

    # Per-routine-block sub-task lookup. Fetch the children of each routine
    # block from the SAME Tasks DB via the Parent Task self-relation, scoped
    # to non-Done tasks (anything still planned for the upcoming cycle). Used
    # by the 9 PM planning pass to surface "already planned for this block"
    # before drafting additions, per docs/hermes/daily-plan-phase.md.
    routine_lookups: dict[str, dict] = {}
    if routine_pages:
        try:
            # Fan out: for each routine, fetch its non-Done sub-tasks. Bounded
            # concurrency so a workspace with 50 routine blocks doesn't fan
            # out 50 simultaneous Notion calls (the client's rate limiter
            # would serialize them, but pending-coroutine churn is wasteful).
            sem = asyncio.Semaphore(8)

            async def _gather_subs(routine_page: dict) -> tuple[str, list[dict]]:
                routine_id = routine_page.get("id", "")
                if not routine_id:
                    return "", []
                async with sem:
                    try:
                        sub_pages = await app.client.query_all(
                            app.config.tasks_ds_id,
                            filter={
                                "and": [
                                    {
                                        "property": "Parent Task",
                                        "relation": {"contains": routine_id},
                                    },
                                    not_done,
                                ]
                            },
                        )
                    except NotionAPIError:
                        return routine_id, []
                return routine_id, [
                    format_task(
                        p,
                        project_lookup=project_lookup,
                        tag_lookup=tag_lookup,
                        location_property_name=loc_name,
                    )
                    for p in sub_pages
                ]

            sub_results = await asyncio.gather(
                *(_gather_subs(p) for p in routine_pages if p.get("id"))
            )
            for routine_id, subs in sub_results:
                if routine_id:
                    routine_lookups[routine_id] = {
                        "planned_subtasks": subs,
                    }
        except NotionAPIError:
            # Sub-task fetch is optional; core buckets already succeeded.
            routine_lookups = {}
    else:
        routine_lookups = {}

    # Outstanding = dedup union of overdue_or_due_today ∪ on_my_day, preserving
    # the overdue-bucket ordering first.
    seen: set[str] = set()
    outstanding: list[dict] = []
    for task in overdue_or_due_today + on_my_day:
        tid = task.get("id")
        if not tid or tid in seen:
            continue
        seen.add(tid)
        outstanding.append(task)

    schema = app.tasks_schema
    return {
        "now": now_dt.isoformat(timespec="seconds"),
        "timezone": app.config.timezone,
        "buckets": {
            "completed_today": completed_today,
            "overdue_or_due_today": overdue_or_due_today,
            "due_tomorrow": due_tomorrow,
            "on_my_day": on_my_day,
            "inbox": inbox,
            "routine_blocks": routine_blocks,
        },
        "outstanding": outstanding,
        "lookups": {
            "projects": project_lookup,
            "area_tags": tag_lookup,
            "routine_blocks": routine_lookups,
        },
        "task_schema": {
            "has_location_property": schema.has_location_property,
            "location_property_name": schema.location_property_name,
            "location_property_type": schema.location_property_type,
            "location_options": list(schema.location_options),
            "labels_options": list(schema.labels_options),
        },
        "truncated": {
            "completed_today": t_completed,
            "overdue_or_due_today": t_overdue,
            "due_tomorrow": t_tomorrow,
            "on_my_day": t_my_day,
            "inbox": t_inbox,
            "routine_blocks": t_routine,
        },
    }


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def weekly_review_snapshot(
    days_back: Annotated[
        int, Field(description="How many days back to look for completed tasks.", ge=1, le=90)
    ] = 7,
    ctx: Context = None,
) -> dict:
    """Canonical weekly-cadence review: completed tasks over the period, all overdue
    tasks, active projects (with Progress/Meta), active goals, and milestones (if
    configured). Unlike daily_review_snapshot (task-bucket focused), this exists so
    Goals/Milestones get checked on a regular cadence too — goals that are never
    revisited between quarterly check-ins are a known failure mode this closes.

    Use this once a week (or whatever cadence fits); use daily_review_snapshot for
    the day-to-day task-triage view.
    """
    app = _ctx(ctx)
    tz = ZoneInfo(app.config.timezone)
    now_dt = datetime.now(tz)
    today_iso = now_dt.date().isoformat()
    period_start = (now_dt.date() - timedelta(days=days_back)).isoformat()

    not_done = {"property": "Status", "status": {"does_not_equal": "Done"}}
    completed_filter = {
        "and": [
            {"property": "Status", "status": {"equals": "Done"}},
            {"property": "Completed", "date": {"on_or_after": period_start}},
        ]
    }
    overdue_filter = {"and": [not_done, {"property": "Due", "date": {"on_or_before": today_iso}}]}
    active_projects_filter = {
        "or": [
            {"property": "Status", "status": {"equals": "Doing"}},
            {"property": "Status", "status": {"equals": "Ongoing"}},
        ]
    }
    active_goals_filter = {"property": "Status", "status": {"equals": "Active"}}
    milestones_ds_id = app.config.secondary_ds.get("Milestones")

    fetches = [
        app.client.query_all(app.config.tasks_ds_id, filter=completed_filter),
        app.client.query_all(app.config.tasks_ds_id, filter=overdue_filter),
        app.client.query_all(app.config.projects_ds_id, filter=active_projects_filter),
        app.client.query_all(app.config.goals_ds_id, filter=active_goals_filter),
    ]
    if milestones_ds_id:
        fetches.append(app.client.query_all(milestones_ds_id))

    try:
        fetched = await asyncio.gather(*fetches)
    except NotionAPIError as e:
        return _handle_api_error(e)

    completed_pages, overdue_pages, project_pages, goal_pages = fetched[:4]
    milestone_pages = fetched[4] if milestones_ds_id else []

    loc_name = app.tasks_schema.location_property_name
    cap = _SNAPSHOT_BUCKET_CAP

    def _fmt_tasks(pages: list[dict]) -> tuple[list[dict], bool]:
        truncated = len(pages) > cap
        return (
            [format_task(p, location_property_name=loc_name) for p in pages[:cap]],
            truncated,
        )

    completed, t_completed = _fmt_tasks(completed_pages)
    overdue, t_overdue = _fmt_tasks(overdue_pages)

    return {
        "period": {"from": period_start, "to": today_iso, "days_back": days_back},
        "completed_this_period": completed,
        "overdue": overdue,
        "active_projects": [format_project(p) for p in project_pages[:cap]],
        "active_goals": [format_goal(p) for p in goal_pages[:cap]],
        "milestones": (
            [
                format_milestone(p, goal_property_name=app.milestones_schema.goal_property_name)
                for p in milestone_pages[:cap]
            ]
            if milestones_ds_id
            else None
        ),
        "truncated": {
            "completed_this_period": t_completed,
            "overdue": t_overdue,
            "active_projects": len(project_pages) > cap,
            "active_goals": len(goal_pages) > cap,
            "milestones": len(milestone_pages) > cap if milestones_ds_id else False,
        },
    }


class BulkTaskUpdate(BaseModel):
    """One row in a bulk_update_tasks call. Mirrors update_task parameters."""

    task_id: str = Field(description="Task page ID, e.g. 'task_abc123'.")
    name: str | None = Field(default=None, description="New task name.")
    status: Literal["To Do", "Doing", "Done"] | None = Field(
        default=None, description="New status."
    )
    due: str | None = Field(
        default=None,
        description=(
            "New due date, YYYY-MM-DD, or a full ISO 8601 datetime for time-blocking, "
            "paired with due_end."
        ),
    )
    due_end: str | None = Field(
        default=None,
        description="End of the time block, ISO 8601 datetime. Only meaningful with due set.",
    )
    priority: Literal["Low", "Medium", "High"] | None = Field(
        default=None, description="New priority."
    )
    project_id: str | None = Field(default=None, description="New project page ID.")
    labels: list[str] | None = Field(default=None, description="New labels (replaces existing).")
    my_day: bool | None = Field(default=None, description="Set My Day flag.")
    parent_task_id: str | None = Field(default=None, description="New parent task page ID.")
    tag_ids: list[str] | None = Field(
        default=None, description="New Tag relation IDs (replaces existing)."
    )
    location: str | None = Field(
        default=None,
        description=(
            "Sets the Tasks Location property. Auto-detects select / multi_select / status. "
            "Ignored if Tasks has no Location property — see "
            "daily_review_snapshot.task_schema.has_location_property."
        ),
    )
    enforce_schedule: bool | None = Field(
        default=None, description="Sets the Enforce Schedule checkbox for recurring tasks."
    )
    recur_interval: int | None = Field(
        default=None, description="Recur Interval (number). Pairs with recur_unit."
    )
    recur_unit: str | None = Field(
        default=None,
        description=f"Recur Unit (select). Canonical options: {', '.join(RECUR_UNITS)}.",
    )
    days: list[str] | None = Field(
        default=None,
        description="Days (multi_select of weekday short names, e.g. ['Mon', 'Wed']).",
    )
    snooze_until: str | None = Field(
        default=None,
        description="Snooze Until (YYYY-MM-DD). Shifts Due and sets Snooze Until. "
        "If both due and snooze_until are set, snooze_until wins.",
    )


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def bulk_update_tasks(
    updates: Annotated[
        list[BulkTaskUpdate],
        Field(description="List of per-task patches. Each follows the BulkTaskUpdate shape."),
    ],
    ctx: Context = None,
) -> dict:
    """Apply multiple task patches in a single call. Each update follows the same
    shape as update_task plus tag_ids and location. Runs concurrently under the
    Notion rate limiter; never raises on a single failure.

    Use this at the END of a daily review or any workflow that updates more than
    ~3 tasks at once. For a single task, use update_task instead.

    Returns:
      results — list of one entry per input update, in order:
        {task_id, ok: true,  task: {formatted task dict}}     on success
        {task_id, ok: false, error: 'human-readable reason'}  on failure
      summary — {ok: N, failed: N, total: N}

    Failures are per-row and self-describing — surface them to the user, retry the
    failed rows, or skip them. The whole call never raises; ok=false rows are
    surfaced through results, not through an exception.
    """
    app = _ctx(ctx)

    if not updates:
        return {
            "results": [],
            "summary": {"ok": 0, "failed": 0, "total": 0},
        }

    sem = asyncio.Semaphore(_BULK_UPDATE_CONCURRENCY)

    async def _apply_one(idx: int, update: BulkTaskUpdate) -> dict:
        async with sem:
            props: dict = {}
            warnings: list[str] = []

            if update.name is not None:
                props["Name"] = _prop_title(update.name)
            if update.status is not None:
                props["Status"] = _prop_status(update.status)
            if update.due is not None:
                props["Due"] = _prop_date(update.due, update.due_end)
            if update.priority is not None:
                props["Priority"] = _prop_status(update.priority)
            if update.project_id is not None:
                props["Project"] = _prop_relation([update.project_id])
            if update.labels is not None:
                props["Labels"] = _prop_multi_select(update.labels)
            if update.my_day is not None:
                props["My Day"] = _prop_checkbox(update.my_day)
            if update.parent_task_id is not None:
                props["Parent Task"] = _prop_relation([update.parent_task_id])
            if update.tag_ids is not None:
                props["Tag"] = _prop_relation(update.tag_ids)
            if update.enforce_schedule is not None:
                props["Enforce Schedule"] = _prop_checkbox(update.enforce_schedule)
            if update.recur_interval is not None:
                props["Recur Interval"] = _prop_number(update.recur_interval)
            if update.recur_unit is not None:
                props["Recur Unit"] = _prop_select(update.recur_unit)
            if update.days is not None:
                props["Days"] = _prop_multi_select(update.days)
            if update.snooze_until is not None:
                props["Due"] = _prop_date(update.snooze_until)
                props["Snooze Until"] = _prop_date(update.snooze_until)
            if update.location is not None:
                payload, warning = _build_location_payload(app.tasks_schema, update.location)
                if payload is not None and app.tasks_schema.location_property_name:
                    props[app.tasks_schema.location_property_name] = payload
                if warning:
                    warnings.append(warning)

            if not props:
                return {
                    "task_id": update.task_id,
                    "ok": False,
                    "error": ("No properties to update. Provide at least one field."),
                }

            try:
                page = await app.client.update_page(update.task_id, props)
                row: dict = {
                    "task_id": update.task_id,
                    "ok": True,
                    "task": format_task(
                        page,
                        location_property_name=app.tasks_schema.location_property_name,
                    ),
                }
                if warnings:
                    row["_warnings"] = warnings
                return row
            except NotionAPIError as e:
                err = _handle_api_error(e, "Use search_tasks to find valid task IDs.")
                return {
                    "task_id": update.task_id,
                    "ok": False,
                    "error": err.get("error", str(e)),
                }

    results = await asyncio.gather(*(_apply_one(i, u) for i, u in enumerate(updates)))
    ok_count = sum(1 for r in results if r.get("ok"))
    failed_count = len(results) - ok_count
    return {
        "results": results,
        "summary": {"ok": ok_count, "failed": failed_count, "total": len(results)},
    }


class BulkTaskCreate(BaseModel):
    """One row in a bulk_create_tasks call. Mirrors create_task parameters."""

    name: str = Field(description="Task name.")
    status: Literal["To Do", "Doing", "Done"] | None = Field(
        default=None, description="Status. Defaults to To Do."
    )
    due: str | None = Field(
        default=None,
        description=(
            "Due date, YYYY-MM-DD, or a full ISO 8601 datetime for time-blocking, paired with due_end."
        ),
    )
    due_end: str | None = Field(
        default=None,
        description="End of the time block, ISO 8601 datetime. Only meaningful with due set.",
    )
    priority: Literal["Low", "Medium", "High"] | None = Field(default=None, description="Priority.")
    project_id: str | None = Field(
        default=None, description="Project page ID to link this task to."
    )
    labels: list[str] | None = Field(default=None, description="Label names (multi-select).")
    my_day: bool | None = Field(default=None, description="Add to My Day.")
    parent_task_id: str | None = Field(
        default=None, description="Parent task page ID (for sub-tasks)."
    )
    tag_ids: list[str] | None = Field(
        default=None, description="Tag page IDs to link via the Tag relation."
    )
    location: str | None = Field(
        default=None,
        description="Sets the Tasks Location property. Auto-detects select / multi_select / status.",
    )
    enforce_schedule: bool | None = Field(
        default=None, description="Sets the Enforce Schedule checkbox for recurring tasks."
    )
    recur_interval: int | None = Field(
        default=None, description="Recur Interval (number). Pairs with recur_unit."
    )
    recur_unit: str | None = Field(
        default=None,
        description=f"Recur Unit (select). Canonical options: {', '.join(RECUR_UNITS)}.",
    )
    days: list[str] | None = Field(
        default=None,
        description="Days (multi_select of weekday short names, e.g. ['Mon', 'Wed']).",
    )
    snooze_until: str | None = Field(
        default=None,
        description="Snooze Until (YYYY-MM-DD). Shifts Due and sets Snooze Until. "
        "If both due and snooze_until are set, snooze_until wins.",
    )
    content: str | None = Field(
        default=None,
        description="Page body content as markdown (same syntax as create_task).",
    )


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def bulk_create_tasks(
    creates: Annotated[
        list[BulkTaskCreate],
        Field(description="List of per-task creates. Each follows the BulkTaskCreate shape."),
    ],
    ctx: Context = None,
) -> dict:
    """Create multiple tasks in a single call. Each row follows the same shape as
    create_task. Concurrency-limited to _BULK_UPDATE_CONCURRENCY; never raises on
    a single row failure.

    Returns:
      results — list of one entry per input create, in order:
        {row_index, ok: true,  task: {formatted task dict}}     on success
        {row_index, ok: false, error: 'human-readable reason'}  on failure
      summary — {ok: N, failed: N, total: N}

    Note: returns row_index (not task_id) because creates have no pre-existing ID.
    Failures are per-row and self-describing — the whole call never raises.
    """
    app = _ctx(ctx)

    if not creates:
        return {
            "results": [],
            "summary": {"ok": 0, "failed": 0, "total": 0},
        }

    sem = asyncio.Semaphore(_BULK_UPDATE_CONCURRENCY)

    async def _create_one(idx: int, create: BulkTaskCreate) -> dict:
        async with sem:
            props: dict = {"Name": _prop_title(create.name)}
            warnings: list[str] = []

            if create.status:
                props["Status"] = _prop_status(create.status)
            if create.due:
                props["Due"] = _prop_date(create.due, create.due_end)
            if create.priority:
                props["Priority"] = _prop_status(create.priority)
            if create.project_id:
                props["Project"] = _prop_relation([create.project_id])
            if create.labels:
                props["Labels"] = _prop_multi_select(create.labels)
            if create.my_day:
                props["My Day"] = _prop_checkbox(True)
            if create.parent_task_id:
                props["Parent Task"] = _prop_relation([create.parent_task_id])
            if create.tag_ids:
                props["Tag"] = _prop_relation(create.tag_ids)
            if create.enforce_schedule is not None:
                props["Enforce Schedule"] = _prop_checkbox(create.enforce_schedule)
            if create.recur_interval is not None:
                props["Recur Interval"] = _prop_number(create.recur_interval)
            if create.recur_unit is not None:
                props["Recur Unit"] = _prop_select(create.recur_unit)
            if create.days:
                props["Days"] = _prop_multi_select(create.days)
            if create.snooze_until:
                props["Due"] = _prop_date(create.snooze_until)
                props["Snooze Until"] = _prop_date(create.snooze_until)
            if create.location is not None:
                payload, warning = _build_location_payload(app.tasks_schema, create.location)
                if payload is not None and app.tasks_schema.location_property_name:
                    props[app.tasks_schema.location_property_name] = payload
                if warning:
                    warnings.append(warning)

            children = text_to_blocks(create.content) if create.content else None

            try:
                page = await app.client.create_page(
                    app.config.tasks_ds_id, props, children=children
                )
                row: dict = {
                    "row_index": idx,
                    "ok": True,
                    "task": format_task(
                        page,
                        location_property_name=app.tasks_schema.location_property_name,
                    ),
                }
                if warnings:
                    row["_warnings"] = warnings
                return row
            except NotionAPIError as e:
                err = _handle_api_error(e, "Check that project/parent IDs are valid.")
                return {"row_index": idx, "ok": False, "error": err.get("error", str(e))}

    results = await asyncio.gather(*(_create_one(i, c) for i, c in enumerate(creates)))
    ok_count = sum(1 for r in results if r.get("ok"))
    failed_count = len(results) - ok_count
    return {
        "results": results,
        "summary": {"ok": ok_count, "failed": failed_count, "total": len(results)},
    }


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def clear_my_day(ctx: Context = None) -> dict:
    """Unset My Day on every task currently flagged for it. Thin wrapper over the
    same bulk-update machinery as bulk_update_tasks — finds all My Day tasks and
    unsets the flag on each in one call, for end-of-day/reset workflows."""
    app = _ctx(ctx)
    try:
        pages = await app.client.query_all(
            app.config.tasks_ds_id,
            filter={"property": "My Day", "checkbox": {"equals": True}},
        )
    except NotionAPIError as e:
        return _handle_api_error(e)

    if not pages:
        return {"results": [], "summary": {"ok": 0, "failed": 0, "total": 0}}

    sem = asyncio.Semaphore(_BULK_UPDATE_CONCURRENCY)

    async def _clear_one(page: dict) -> dict:
        task_id = page.get("id", "")
        async with sem:
            try:
                updated = await app.client.update_page(task_id, {"My Day": _prop_checkbox(False)})
                return {
                    "task_id": task_id,
                    "ok": True,
                    "task": format_task(
                        updated, location_property_name=app.tasks_schema.location_property_name
                    ),
                }
            except NotionAPIError as e:
                err = _handle_api_error(e)
                return {"task_id": task_id, "ok": False, "error": err.get("error", str(e))}

    results = await asyncio.gather(*(_clear_one(p) for p in pages))
    ok_count = sum(1 for r in results if r.get("ok"))
    return {
        "results": results,
        "summary": {"ok": ok_count, "failed": len(results) - ok_count, "total": len(results)},
    }


# =========================================================================
#  MILESTONES & WORK SESSIONS
# =========================================================================


def _secondary_ds_id(app: AppContext, name: str) -> str | None:
    return app.config.secondary_ds.get(name)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def search_milestones(
    query: Annotated[
        str | None, Field(description="Text to search for in milestone names.")
    ] = None,
    goal_id: Annotated[
        str | None,
        Field(description="Filter by linked goal page ID. Only works if a Goal relation exists."),
    ] = None,
    limit: Annotated[int, Field(description="Maximum results.", ge=1, le=100)] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """Search Milestones by name and, if this workspace has one, the Goal relation.
    Milestones' schema varies by workspace — only Name is guaranteed; Date Completed/
    Target Deadline/Goal are surfaced when present."""
    app = _ctx(ctx)
    ds_id = _secondary_ds_id(app, "Milestones")
    if not ds_id:
        return _error("Milestones database not configured. Set UB_MILESTONES_DS_ID in .env.")

    schema = app.milestones_schema
    filters: list[dict] = []
    if query:
        filters.append({"property": "Name", "title": {"contains": query}})
    if goal_id:
        if not schema.goal_property_name:
            return _error("This workspace's Milestones database has no Goal relation to filter by.")
        filters.append({"property": schema.goal_property_name, "relation": {"contains": goal_id}})

    filter_obj: dict | None = None
    if len(filters) == 1:
        filter_obj = filters[0]
    elif len(filters) > 1:
        filter_obj = {"and": filters}

    try:
        pages = await app.client.query_all(ds_id, filter=filter_obj)
        return [
            format_milestone(p, goal_property_name=schema.goal_property_name) for p in pages[:limit]
        ]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def create_milestone(
    name: Annotated[str, Field(description="Milestone name.")],
    goal_id: Annotated[
        str | None,
        Field(
            description="Goal page ID to link. Only works if this workspace has a Goal relation."
        ),
    ] = None,
    date_completed: Annotated[
        str | None,
        Field(description="Date Completed (YYYY-MM-DD). Only set if this property exists."),
    ] = None,
    target_deadline: Annotated[
        str | None,
        Field(description="Target Deadline (YYYY-MM-DD). Only set if this property exists."),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Create a Milestone. goal_id/date_completed/target_deadline are only applied if
    this workspace's Milestones database actually has those properties — check the
    response's _warning field if one was silently skipped."""
    app = _ctx(ctx)
    ds_id = _secondary_ds_id(app, "Milestones")
    if not ds_id:
        return _error("Milestones database not configured. Set UB_MILESTONES_DS_ID in .env.")

    schema = app.milestones_schema
    props: dict = {"Name": _prop_title(name)}
    warnings: list[str] = []

    if goal_id:
        if schema.goal_property_name:
            props[schema.goal_property_name] = _prop_relation([goal_id])
        else:
            warnings.append("goal_id ignored — no Goal relation on this workspace's Milestones DB.")
    if date_completed:
        if schema.has_date_completed:
            props["Date Completed"] = _prop_date(date_completed)
        else:
            warnings.append("date_completed ignored — no Date Completed property found.")
    if target_deadline:
        if schema.has_target_deadline:
            props["Target Deadline"] = _prop_date(target_deadline)
        else:
            warnings.append("target_deadline ignored — no Target Deadline property found.")

    try:
        page = await app.client.create_page(ds_id, props)
        result = format_milestone(page, goal_property_name=schema.goal_property_name)
        if warnings:
            result["_warning"] = " ".join(warnings)
        return result
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def update_milestone(
    milestone_id: Annotated[str, Field(description="Milestone page ID to update.")],
    name: Annotated[str | None, Field(description="New milestone name.")] = None,
    goal_id: Annotated[str | None, Field(description="New goal page ID (if supported).")] = None,
    date_completed: Annotated[
        str | None, Field(description="New Date Completed (YYYY-MM-DD, if supported).")
    ] = None,
    target_deadline: Annotated[
        str | None, Field(description="New Target Deadline (YYYY-MM-DD, if supported).")
    ] = None,
    ctx: Context = None,
) -> dict:
    """Update a Milestone. Only provided fields are changed; fields not supported by
    this workspace's Milestones schema are ignored with a _warning."""
    app = _ctx(ctx)
    schema = app.milestones_schema
    props: dict = {}
    warnings: list[str] = []

    if name is not None:
        props["Name"] = _prop_title(name)
    if goal_id is not None:
        if schema.goal_property_name:
            props[schema.goal_property_name] = _prop_relation([goal_id])
        else:
            warnings.append("goal_id ignored — no Goal relation on this workspace's Milestones DB.")
    if date_completed is not None:
        if schema.has_date_completed:
            props["Date Completed"] = _prop_date(date_completed)
        else:
            warnings.append("date_completed ignored — no Date Completed property found.")
    if target_deadline is not None:
        if schema.has_target_deadline:
            props["Target Deadline"] = _prop_date(target_deadline)
        else:
            warnings.append("target_deadline ignored — no Target Deadline property found.")

    if not props:
        return _error("No properties to update. Provide at least one field.")

    try:
        page = await app.client.update_page(milestone_id, props)
        result = format_milestone(page, goal_property_name=schema.goal_property_name)
        if warnings:
            result["_warning"] = " ".join(warnings)
        return result
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_milestones to find valid milestone IDs.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def search_work_sessions(
    task_id: Annotated[str | None, Field(description="Filter by linked task page ID.")] = None,
    active_only: Annotated[
        bool, Field(description="Only sessions with no End set (currently in progress).")
    ] = False,
    limit: Annotated[int, Field(description="Maximum results.", ge=1, le=100)] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """Search Work Sessions. Use active_only=true to find any session currently
    running (no End timestamp yet) — useful for 'am I tracking time right now'."""
    app = _ctx(ctx)
    ds_id = _secondary_ds_id(app, "Work Sessions")
    if not ds_id:
        return _error("Work Sessions database not configured. Set UB_WORK_SESSIONS_DS_ID in .env.")

    filters: list[dict] = []
    if task_id:
        filters.append({"property": "Tasks", "relation": {"contains": task_id}})
    if active_only:
        filters.append({"property": "End", "date": {"is_empty": True}})

    filter_obj: dict | None = None
    if len(filters) == 1:
        filter_obj = filters[0]
    elif len(filters) > 1:
        filter_obj = {"and": filters}

    try:
        pages = await app.client.query_all(
            ds_id, filter=filter_obj, sorts=[{"property": "Start", "direction": "descending"}]
        )
        return [format_work_session(p) for p in pages[:limit]]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def log_work_session(
    start: Annotated[
        str, Field(description="Start timestamp, ISO 8601 (e.g. '2026-08-08T14:00:00').")
    ],
    end: Annotated[
        str | None,
        Field(description="End timestamp, ISO 8601. Omit to start an in-progress session."),
    ] = None,
    task_id: Annotated[
        str | None, Field(description="Task page ID to link this session to.")
    ] = None,
    name: Annotated[
        str | None, Field(description="Session name. Defaults to 'Work Session'.")
    ] = None,
    ctx: Context = None,
) -> dict:
    """Log a Work Session — with just `start`, begins an in-progress session (no End);
    with `start` and `end`, logs a completed session. Notion computes Duration."""
    app = _ctx(ctx)
    ds_id = _secondary_ds_id(app, "Work Sessions")
    if not ds_id:
        return _error("Work Sessions database not configured. Set UB_WORK_SESSIONS_DS_ID in .env.")

    props: dict = {
        "Name": _prop_title(name or "Work Session"),
        "Start": _prop_date(start),
    }
    if end:
        props["End"] = _prop_date(end)
    if task_id:
        props["Tasks"] = _prop_relation([task_id])

    try:
        page = await app.client.create_page(ds_id, props)
        return format_work_session(page)
    except NotionAPIError as e:
        return _handle_api_error(e, "Check that task_id is valid and timestamps are ISO 8601.")


# =========================================================================
#  GENERIC — Secondary Databases (4 tools)
# =========================================================================


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def query_database(
    database: Annotated[
        str | None,
        Field(
            description="Configured secondary database name (e.g. 'Work Sessions', 'Books', 'People'). "
            "Mutually exclusive with data_source_id — use data_source_id for ad-hoc DBs not in the env map."
        ),
    ] = None,
    data_source_id: Annotated[
        str | None,
        Field(
            description="Direct data source ID to query. Use this for databases not in the configured "
            "SECONDARY_DB_ENV_MAP. Discover IDs via list_databases."
        ),
    ] = None,
    filter: Annotated[
        dict | None,
        Field(description="Notion filter object. See Notion API docs for filter syntax."),
    ] = None,
    sorts: Annotated[
        list[dict] | None,
        Field(
            description="Notion sorts array. E.g. [{'property': 'Name', 'direction': 'ascending'}]"
        ),
    ] = None,
    limit: Annotated[int, Field(description="Maximum results.", ge=1, le=100)] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """Query any Notion database — configured secondary DBs by name, or any DB by data_source_id.

    Pass exactly one of ``database`` or ``data_source_id``. With no arguments, returns the
    list of configured secondary DBs plus a hint pointing at ``list_databases`` for ad-hoc
    discovery. Accepts optional Notion ``filter`` and ``sorts``. For primary databases
    (Tasks, Projects, Notes, Tags, Goals), use the dedicated tools instead."""
    app = _ctx(ctx)

    if database and data_source_id:
        return _error("Pass exactly one of 'database' or 'data_source_id', not both.")

    if not database and not data_source_id:
        available = list(app.config.secondary_ds.keys())
        return {
            "configured_databases": available,
            "hint": "For ad-hoc databases not in this list, call list_databases first, then "
            "pass the returned data_source_id here.",
        }

    if data_source_id:
        ds_id: str = data_source_id
    else:
        resolved = app.config.secondary_ds.get(database or "")
        if not resolved:
            available = list(app.config.secondary_ds.keys())
            return _error(
                f"Database '{database}' not found in configured secondary DBs. "
                f"Configured: {', '.join(available) if available else 'none'}. "
                f"For unconfigured DBs, call list_databases and pass data_source_id instead."
            )
        ds_id = resolved

    try:
        pages = await app.client.query_all(ds_id, filter=filter, sorts=sorts)
        return [format_generic_page(p) for p in pages[:limit]]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def list_databases(
    query: Annotated[
        str | None,
        Field(description="Optional title substring to filter by (case-insensitive)."),
    ] = None,
    limit: Annotated[int, Field(description="Maximum results.", ge=1, le=100)] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """List every Notion database shared with the integration.

    Discovers databases via Notion's ``/search`` endpoint with ``object=database``.
    Use this to find databases that aren't in the configured SECONDARY_DB_ENV_MAP —
    then pass the returned ``id`` (or one of its ``data_sources[].id``) to
    ``query_database``, ``create_page``, or ``get_database_schema``.

    Read-only. The integration must already have access to the database."""
    app = _ctx(ctx)
    try:
        results = await app.client.search(
            query or "",
            filter={"value": "database", "property": "object"},
        )
    except NotionAPIError as e:
        return _handle_api_error(e)

    databases: list[dict] = []
    for db in results[:limit]:
        title_parts = db.get("title", [])
        title = "".join(t.get("plain_text", "") for t in title_parts) if title_parts else ""
        if query and query.lower() not in title.lower():
            continue
        databases.append(
            {
                "id": db.get("id"),
                "title": title,
                "url": db.get("url"),
                "archived": bool(db.get("archived")),
                "last_edited": db.get("last_edited_time"),
                "data_sources": db.get("data_sources"),
                "icon": db.get("icon"),
            }
        )
    return databases


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def create_page(
    data_source_id: Annotated[
        str,
        Field(
            description="Data source ID to create the page in. Discover via list_databases or get_database_schema."
        ),
    ],
    properties: Annotated[
        dict,
        Field(
            description=(
                "Property values. Two shapes accepted per property:\n"
                "  • Simple value (auto-coerced): str for title/rich_text/select/status/url/email/phone_number, "
                "list[str] for multi_select or relation IDs, bool for checkbox, "
                "int/float for number, {'start': 'YYYY-MM-DD'[, 'end': ...]} for date.\n"
                "  • Raw Notion property payload (passed through verbatim): e.g. "
                "{'Status': {'select': {'name': 'Active'}}}, {'Cover': {'files': [...]}}, "
                "{'Formula': {'formula': {'expression': '...'}}}. Use raw form for property types "
                "the simple coercion doesn't support (files, formulas, verification, rollups)."
            )
        ),
    ],
    content: Annotated[
        str | None,
        Field(
            description="Optional page body as markdown. Parsed to Notion blocks "
            "(headings, bullets, numbered lists, todos, code, quotes, dividers, paragraphs)."
        ),
    ] = None,
    template: Annotated[
        dict | None,
        Field(
            description="Optional template to apply, e.g. {'type': 'default'} or "
            "{'type': 'template_id', 'template_id': '...'}. Mutually exclusive with content."
        ),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Create a page in any Notion data source — the generic CRUD fallback for databases
    that don't have a dedicated MCP tool.

    Validates property names against the data source's schema (clear errors for typos),
    auto-coerces simple values, passes raw Notion property payloads through verbatim.
    Use ``list_databases`` to discover ``data_source_id`` for unconfigured DBs.

    DESTRUCTIVE in the sense that a malformed call creates a permanent page in the
    target DB. Use ``[TEST]``-prefixed names against a sandbox workspace first."""
    app = _ctx(ctx)

    if content is not None and template is not None:
        return _error("Pass at most one of 'content' or 'template', not both.")

    # Fetch the data source schema to validate property names and types.
    try:
        ds = await app.client.get_data_source(data_source_id)
    except NotionAPIError as e:
        return _handle_api_error(
            e, "Check that data_source_id is valid and shared with the integration."
        )

    schema_props = ds.get("properties", {})

    notion_props: dict = {}
    for prop_name, value in properties.items():
        if prop_name not in schema_props:
            return _error(
                f"Property '{prop_name}' not found on this data source. "
                f"Available: {', '.join(schema_props.keys())}"
            )

        # Raw Notion payload — pass through verbatim
        if isinstance(value, dict):
            notion_props[prop_name] = value
            continue

        # Simple value — coerce based on schema-declared type
        ptype = schema_props[prop_name].get("type")
        try:
            notion_props[prop_name] = _coerce_property(ptype, value)
        except ValueError as ve:
            return _error(f"Cannot set '{prop_name}': {ve}")

    children: list[dict] | None = None
    if content is not None:
        try:
            children = text_to_blocks(content)
        except Exception as e:
            return _error(f"Failed to parse content markdown: {e}")

    try:
        page = await app.client.create_page(
            data_source_id,
            notion_props,
            children=children,
            template=template,
        )
    except NotionAPIError as e:
        return _handle_api_error(e)

    formatted = format_generic_page(page)
    formatted["url"] = page.get("url")
    return formatted


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_page(
    page_id: Annotated[str, Field(description="Any Notion page ID.")],
    ctx: Context = None,
) -> dict:
    """Fetch any page by ID and return all properties auto-formatted.
    Works for any database — primary or secondary."""
    app = _ctx(ctx)
    try:
        page = await app.client.get_page(page_id)
        return format_generic_page(page)
    except NotionAPIError as e:
        return _handle_api_error(e, "Check the page ID is valid.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_page_content(
    page_id: Annotated[str, Field(description="Any Notion page ID.")],
    ctx: Context = None,
) -> dict:
    """Get any page's properties plus its full body content as enhanced Markdown.
    Works for any page type. For notes specifically, get_note_content returns
    the same data with note-specific formatting."""
    app = _ctx(ctx)
    try:
        page, content = await asyncio.gather(
            app.client.get_page(page_id),
            _read_page_markdown(app, page_id),
        )
        result = format_generic_page(page)
        result["content"] = content
        return result
    except NotionAPIError as e:
        return _handle_api_error(e, "Check the page ID is valid.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def update_page(
    page_id: Annotated[str, Field(description="Page ID to update.")],
    properties: Annotated[
        dict,
        Field(
            description=(
                "Dict of property name → value. Auto-coerces types: "
                "str for title/rich_text/select/status, list[str] for multi_select, "
                "bool for checkbox, float/int for number, "
                "{'start': 'YYYY-MM-DD'} for date, list[str] for relation IDs."
            )
        ),
    ],
    ctx: Context = None,
) -> dict:
    """Update properties on any page by ID. Accepts a dict of property name → value
    with auto type coercion. For primary database items, prefer the dedicated update tools."""
    app = _ctx(ctx)

    # First fetch the page to learn property types
    try:
        page = await app.client.get_page(page_id)
    except NotionAPIError as e:
        return _handle_api_error(e, "Check the page ID is valid.")

    existing_props = page.get("properties", {})
    notion_props: dict = {}

    for prop_name, value in properties.items():
        if prop_name not in existing_props:
            return _error(
                f"Property '{prop_name}' not found on this page. "
                f"Available: {', '.join(existing_props.keys())}"
            )

        ptype = existing_props[prop_name].get("type")
        try:
            notion_props[prop_name] = _coerce_property(ptype, value)
        except ValueError as ve:
            return _error(f"Cannot set '{prop_name}': {ve}")

    try:
        updated = await app.client.update_page(page_id, notion_props)
        return format_generic_page(updated)
    except NotionAPIError as e:
        return _handle_api_error(e)


def _coerce_property(ptype: str, value) -> dict:
    """Convert a simple value into the Notion property format based on the property type."""
    if ptype == "title":
        return _prop_title(str(value))
    elif ptype == "rich_text":
        return _prop_rich_text(str(value))
    elif ptype == "select":
        return _prop_select(str(value))
    elif ptype == "multi_select":
        if isinstance(value, list):
            return _prop_multi_select([str(v) for v in value])
        return _prop_multi_select([str(value)])
    elif ptype == "status":
        return _prop_status(str(value))
    elif ptype == "checkbox":
        return _prop_checkbox(bool(value))
    elif ptype == "number":
        return _prop_number(float(value))
    elif ptype == "date":
        if isinstance(value, dict):
            return _prop_date(value.get("start", ""), value.get("end"))
        return _prop_date(str(value))
    elif ptype == "url":
        return _prop_url(str(value))
    elif ptype == "relation":
        if isinstance(value, list):
            return _prop_relation([str(v) for v in value])
        return _prop_relation([str(value)])
    else:
        raise ValueError(f"Unsupported property type: {ptype}")


# ---------------------------------------------------------------------------
# People tools (5) — Tier 1 of the Hermes extension
# ---------------------------------------------------------------------------
#
# People is a workspace-customised secondary DB with a stable core property
# surface (Full Name title; Pipeline Status status; Surname / Title / Company /
# Location rich_text; Relationship / Interests multi_select; Birthday /
# Check-In / Last Check-In dates; Email / Phone email/phone_number; Website /
# LinkedIn / Twitter / Instagram url; Tags relation) but property names and
# option sets vary. Tools here introspect the live schema once (see
# AppContext.people_schema) and silently skip fields that aren't present
# rather than 400-ing on every optional field that the workspace trimmed.


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def search_people(
    query: Annotated[str | None, Field(description="Text to search for in full names.")] = None,
    pipeline_status: Annotated[
        str | None,
        Field(
            description=(
                "Filter by Pipeline Status (e.g. 'Prospect', 'Contacted'). "
                f"Canonical options: {', '.join(PIPELINE_STATUSES)}."
            )
        ),
    ] = None,
    relationship: Annotated[
        str | None,
        Field(description="Filter by Relationship (multi_select contains)."),
    ] = None,
    tag_id: Annotated[
        str | None, Field(description="Filter by linked Tag (relation contains).")
    ] = None,
    sort_by: Annotated[
        Literal["name", "last_check_in", "check_in"],
        Field(description="Sort field. Defaults to 'name' ascending."),
    ] = "name",
    limit: Annotated[int, Field(description="Maximum results.", ge=1, le=100)] = 50,
    ctx: Context = None,
) -> list[dict] | dict:
    """Search People by name, Pipeline Status, Relationship, or linked tag.
    Sort by name (default), last check-in date, or next check-in date.
    Use get_person_detail for a full view including related tasks / projects /
    recent notes. People database must be configured (UB_PEOPLE_DS_ID)."""
    app = _ctx(ctx)
    ds_id = _secondary_ds_id(app, "People")
    if not ds_id:
        return _error("People database not configured. Set UB_PEOPLE_DS_ID in .env.")

    if (err := _validate_pipeline_status(app, pipeline_status)) is not None:
        return err

    filters: list[dict] = []
    if query:
        filters.append({"property": "Full Name", "title": {"contains": query}})
    if pipeline_status:
        filters.append({"property": "Pipeline Status", "status": {"equals": pipeline_status}})
    if relationship:
        filters.append({"property": "Relationship", "multi_select": {"contains": relationship}})
    if tag_id:
        filters.append({"property": "Tags", "relation": {"contains": tag_id}})

    filter_obj: dict | None = None
    if len(filters) == 1:
        filter_obj = filters[0]
    elif len(filters) > 1:
        filter_obj = {"and": filters}

    sort_prop = {"name": "Full Name", "last_check_in": "Last Check-In", "check_in": "Check-In"}[
        sort_by
    ]
    sorts = [{"property": sort_prop, "direction": "ascending"}]

    try:
        pages = await app.client.query_all(ds_id, filter=filter_obj, sorts=sorts)
        return [format_generic_page(p) for p in pages[:limit]]
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_person_detail(
    person_id: Annotated[str, Field(description="Person page ID.")],
    resolve_relations: Annotated[
        bool,
        Field(
            description=(
                "If any relation on the person is truncated at Notion's 25-item "
                "inline cap (see _truncated_relations), fetch the full list via "
                "an extra paginated API call instead of leaving it flagged."
            )
        ),
    ] = False,
    ctx: Context = None,
) -> dict:
    """Get a consolidated person view: properties, related tasks, related
    projects, and recent notes. Returns ``person`` plus ``related_tasks``,
    ``related_projects``, and ``recent_notes`` lists. The inverse-relation
    property names used to find tasks/projects/notes (``Person`` singular on
    Tasks/Projects, ``People`` plural on Notes) are workspace-specific — if
    any of those three lists returns empty for a workspace that does have
    related items, the data source schema needs to be checked and the
    filter property name adjusted."""
    app = _ctx(ctx)
    try:
        # Parallel: get person, related tasks, related projects, recent notes.
        # The relation property names are best-guess standard (UB v3.0 uses
        # 'Person' on Tasks/Projects singular and 'People' on Notes plural);
        # if any returns empty in a workspace that actually has related items,
        # inspect the data source schema and adjust the property name.
        person_fut = app.client.get_page(person_id)
        tasks_fut = app.client.query_all(
            app.config.tasks_ds_id,
            filter={"property": "Person", "relation": {"contains": person_id}},
        )
        projects_fut = app.client.query_all(
            app.config.projects_ds_id,
            filter={"property": "Person", "relation": {"contains": person_id}},
        )
        notes_fut = app.client.query_all(
            app.config.notes_ds_id,
            filter={"property": "People", "relation": {"contains": person_id}},
            sorts=[{"property": "Note Date", "direction": "descending"}],
        )
        person_page, task_pages, project_pages, note_pages = await asyncio.gather(
            person_fut, tasks_fut, projects_fut, notes_fut
        )

        person = format_generic_page(person_page)
        if resolve_relations:
            await _resolve_truncated_relations(app, person_page, person)
        loc_name = app.tasks_schema.location_property_name
        tasks = [format_task(t, location_property_name=loc_name) for t in task_pages]
        projects = [format_project(p) for p in project_pages]
        notes = [format_note(n) for n in note_pages[:10]]

        return {
            "person": person,
            "related_tasks": tasks,
            "related_projects": projects,
            "recent_notes": notes,
        }
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_people to find valid person IDs.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def create_person(
    name: Annotated[str, Field(description="Full name (the title property).")],
    surname: Annotated[str | None, Field(description="Surname (rich_text).")] = None,
    title: Annotated[str | None, Field(description="Job title (rich_text).")] = None,
    company: Annotated[str | None, Field(description="Company (rich_text).")] = None,
    location: Annotated[str | None, Field(description="Location (rich_text).")] = None,
    pipeline_status: Annotated[
        str | None,
        Field(
            description=(
                f"Pipeline Status (status). Canonical options: {', '.join(PIPELINE_STATUSES)}. "
                "Live-discovered options validated server-side; values not in the workspace's "
                "list return a 400 from Notion."
            )
        ),
    ] = None,
    relationship: Annotated[
        list[str] | None,
        Field(description="Relationship tags (multi_select, replaces existing)."),
    ] = None,
    interests: Annotated[
        list[str] | None,
        Field(description="Interests (multi_select, replaces existing)."),
    ] = None,
    birthday: Annotated[str | None, Field(description="Birthday (YYYY-MM-DD).")] = None,
    email: Annotated[str | None, Field(description="Email (email type).")] = None,
    phone: Annotated[str | None, Field(description="Phone number (phone_number type).")] = None,
    website: Annotated[str | None, Field(description="Website URL.")] = None,
    linkedin: Annotated[str | None, Field(description="LinkedIn URL.")] = None,
    twitter: Annotated[str | None, Field(description="Twitter / X URL.")] = None,
    instagram: Annotated[str | None, Field(description="Instagram URL.")] = None,
    tag_ids: Annotated[
        list[str] | None, Field(description="Tag page IDs to link via Tags relation.")
    ] = None,
    content: Annotated[
        str | None,
        Field(
            description=(
                "Page body content as markdown. Supports: # headings, - bullets, "
                "1. numbered lists, - [ ] to-dos, ```code blocks```, > quotes, --- "
                "dividers, and plain paragraphs."
            )
        ),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Create a new Person. Use search_people for lookups, search_tags for tag IDs.
    Properties the workspace doesn't carry are silently skipped (see _warnings).
    Surfaces `possible_duplicate` when an exact-name match exists; that warning
    never blocks the create."""
    app = _ctx(ctx)
    ds_id = _secondary_ds_id(app, "People")
    if not ds_id:
        return _error("People database not configured. Set UB_PEOPLE_DS_ID in .env.")

    if (err := _validate_pipeline_status(app, pipeline_status)) is not None:
        return err

    schema = app.people_schema
    props: dict = {"Full Name": _prop_title(name)}
    warnings: list[str] = []

    if surname:
        props["Surname"] = _prop_rich_text(surname)
    if title:
        props["Title"] = _prop_rich_text(title)
    if company:
        props["Company"] = _prop_rich_text(company)
    if location:
        props["Location"] = _prop_rich_text(location)
    if pipeline_status:
        props["Pipeline Status"] = _prop_status(pipeline_status)
    if relationship:
        props["Relationship"] = _prop_multi_select(relationship)
    if interests:
        props["Interests"] = _prop_multi_select(interests)
    if birthday:
        if schema.has_birthday:
            props["Birthday"] = _prop_date(birthday)
        else:
            warnings.append("birthday ignored — no Birthday property found.")
    if email:
        if schema.has_email:
            props["Email"] = _prop_email(email)
        else:
            warnings.append("email ignored — no Email property found.")
    if phone:
        if schema.has_phone:
            props["Phone"] = _prop_phone_number(phone)
        else:
            warnings.append("phone ignored — no Phone property found.")
    if website:
        props["Website"] = _prop_url(website)
    if linkedin:
        props["LinkedIn"] = _prop_url(linkedin)
    if twitter:
        props["Twitter"] = _prop_url(twitter)
    if instagram:
        props["Instagram"] = _prop_url(instagram)
    if tag_ids:
        props["Tags"] = _prop_relation(tag_ids)

    children = text_to_blocks(content) if content else None
    possible_duplicate = await _find_possible_duplicate(app, ds_id, name, format_generic_page)

    try:
        page = await app.client.create_page(ds_id, props, children=children)
        result = format_generic_page(page)
        if warnings:
            result["_warning"] = " ".join(warnings)
        if possible_duplicate:
            result["possible_duplicate"] = possible_duplicate
        return result
    except NotionAPIError as e:
        return _handle_api_error(e)


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=True)
)
async def update_person(
    person_id: Annotated[str, Field(description="Person page ID to update.")],
    name: Annotated[str | None, Field(description="New full name (title).")] = None,
    surname: Annotated[str | None, Field(description="New Surname.")] = None,
    title: Annotated[str | None, Field(description="New job Title.")] = None,
    company: Annotated[str | None, Field(description="New Company.")] = None,
    location: Annotated[str | None, Field(description="New Location.")] = None,
    pipeline_status: Annotated[
        str | None, Field(description="New Pipeline Status (status, validated).")
    ] = None,
    relationship: Annotated[
        list[str] | None,
        Field(description="New Relationship tags (multi_select, replaces existing)."),
    ] = None,
    interests: Annotated[
        list[str] | None,
        Field(description="New Interests (multi_select, replaces existing)."),
    ] = None,
    birthday: Annotated[str | None, Field(description="New Birthday (YYYY-MM-DD).")] = None,
    email: Annotated[str | None, Field(description="New Email.")] = None,
    phone: Annotated[str | None, Field(description="New Phone number.")] = None,
    website: Annotated[str | None, Field(description="New Website URL.")] = None,
    linkedin: Annotated[str | None, Field(description="New LinkedIn URL.")] = None,
    twitter: Annotated[str | None, Field(description="New Twitter URL.")] = None,
    instagram: Annotated[str | None, Field(description="New Instagram URL.")] = None,
    tag_ids: Annotated[
        list[str] | None,
        Field(description="New Tag relation IDs (replaces existing)."),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Update any Person properties. Only provided fields are changed.
    Use search_people to find person IDs. Properties the workspace doesn't
    carry are silently skipped (see _warnings)."""
    app = _ctx(ctx)
    if (err := _validate_pipeline_status(app, pipeline_status)) is not None:
        return err

    schema = app.people_schema
    props: dict = {}
    warnings: list[str] = []

    if name is not None:
        props["Full Name"] = _prop_title(name)
    if surname is not None:
        props["Surname"] = _prop_rich_text(surname)
    if title is not None:
        props["Title"] = _prop_rich_text(title)
    if company is not None:
        props["Company"] = _prop_rich_text(company)
    if location is not None:
        props["Location"] = _prop_rich_text(location)
    if pipeline_status is not None:
        props["Pipeline Status"] = _prop_status(pipeline_status)
    if relationship is not None:
        props["Relationship"] = _prop_multi_select(relationship)
    if interests is not None:
        props["Interests"] = _prop_multi_select(interests)
    if birthday is not None:
        if schema.has_birthday:
            props["Birthday"] = _prop_date(birthday)
        else:
            warnings.append("birthday ignored — no Birthday property found.")
    if email is not None:
        if schema.has_email:
            props["Email"] = _prop_email(email)
        else:
            warnings.append("email ignored — no Email property found.")
    if phone is not None:
        if schema.has_phone:
            props["Phone"] = _prop_phone_number(phone)
        else:
            warnings.append("phone ignored — no Phone property found.")
    if website is not None:
        props["Website"] = _prop_url(website)
    if linkedin is not None:
        props["LinkedIn"] = _prop_url(linkedin)
    if twitter is not None:
        props["Twitter"] = _prop_url(twitter)
    if instagram is not None:
        props["Instagram"] = _prop_url(instagram)
    if tag_ids is not None:
        props["Tags"] = _prop_relation(tag_ids)

    if not props:
        return _error("No properties to update. Provide at least one field.")

    try:
        page = await app.client.update_page(person_id, props)
        result = format_generic_page(page)
        if warnings:
            result["_warning"] = " ".join(warnings)
        return result
    except NotionAPIError as e:
        return _handle_api_error(e, "Use search_people to find valid person IDs.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, idempotentHint=False)
)
async def log_checkin(
    person_id: Annotated[str, Field(description="Person page ID.")],
    summary: Annotated[
        str | None, Field(description="Short summary of the check-in (used as note name prefix).")
    ] = None,
    next_check_in: Annotated[
        str | None, Field(description="Next check-in date (YYYY-MM-DD).")
    ] = None,
    note_type: Annotated[
        str | None,
        Field(
            description=f"Note type for the linked Note. Default 'Meeting'. Options: {', '.join(NOTE_TYPES)}."
        ),
    ] = None,
    content: Annotated[
        str | None,
        Field(description="Page body content for the linked note (markdown)."),
    ] = None,
    ctx: Context = None,
) -> dict:
    """Log a check-in with a person: set ``Last Check-In`` to today (and optionally
    ``Check-In`` to ``next_check_in``), and — when ``content`` or ``summary`` is
    provided — also create a linked Note in the Notes database. Useful for
    Hermes's daily accountability loop.

    Returns ``{person: <format>, note: {id, url, name} | None, _note_create_error?: str}``.
    The note creation is best-effort: if it fails (e.g. the workspace's Notes DB
    has no People relation), the person update still happens and the error is
    surfaced under ``_note_create_error`` rather than thrown."""
    app = _ctx(ctx)
    ds_id = _secondary_ds_id(app, "People")
    if not ds_id:
        return _error("People database not configured. Set UB_PEOPLE_DS_ID in .env.")

    schema = app.people_schema
    props: dict = {}
    warnings: list[str] = []

    if schema.has_last_check_in:
        props["Last Check-In"] = _prop_date(_today())
    else:
        warnings.append("Last Check-In property not found — person left unchanged on date.")
    if next_check_in:
        if schema.has_check_in:
            props["Check-In"] = _prop_date(next_check_in)
        else:
            warnings.append("next_check_in ignored — no Check-In property found.")

    if props:
        try:
            page = await app.client.update_page(person_id, props)
        except NotionAPIError as e:
            return _handle_api_error(e, "Use search_people to find valid person IDs.")
    else:
        try:
            page = await app.client.get_page(person_id)
        except NotionAPIError as e:
            return _handle_api_error(e, "Use search_people to find valid person IDs.")

    person = format_generic_page(page)
    if warnings:
        person["_warning"] = " ".join(warnings)

    result: dict = {"person": person, "note": None}

    if not content and not summary:
        return result

    note_type_value: str | None = note_type or "Meeting"
    if note_type_value not in app.note_types:
        # Fall back to the always-present 'Note' when the workspace's Notes DB
        # doesn't carry the requested type (e.g. 'Meeting' was dropped).
        note_type_value = "Note" if "Note" in app.note_types else next(iter(app.note_types), None)
    if not note_type_value:
        result["_note_create_error"] = "No note_type fell back from the live options list."
        return result

    person_name = person.get("Full Name") or "Person"
    note_name = (
        f"Check-in with {person_name}: {_today()}"
        if not summary
        else f"Check-in with {person_name} — {summary}"
    )

    note_props: dict = {
        "Name": _prop_title(note_name),
        "Note Date": _prop_date(_today()),
        "Type": _prop_select(note_type_value),
    }
    note_blocks = text_to_blocks(content) if content else None

    try:
        note_page = await app.client.create_page(
            app.config.notes_ds_id, note_props, children=note_blocks
        )
        result["note"] = {
            "id": note_page.get("id"),
            "url": note_page.get("url"),
            "name": note_name,
        }
    except NotionAPIError as e:
        err = _handle_api_error(e)
        result["_note_create_error"] = err.get("error", str(e))

    return result


# ---------------------------------------------------------------------------
# Database CRUD tools (3) — Tier 3 of the Hermes extension
# ---------------------------------------------------------------------------
#
# These three tools expose create-database / read-database-schema /
# update-database-schema — used by Hermes's planned onboarding flow that
# auto-generates new UB-style databases under a user-chosen parent page.
# They pin Notion API 2026-03-11 (the rest of the server stays on 2025-09-03)
# which is required for DB-mutation endpoints.
#
# IMPORTANT: these are schema-mutating tools. The integration must have
# permission to write to the parent page. Always test against a sandbox
# workspace before pointing at production UB — create_database makes it
# trivial to spin up orphan databases that pollute the sidebar.


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=True, idempotentHint=False)
)
async def create_database(
    parent_page_id: Annotated[str, Field(description="Parent page ID for the new database.")],
    title: Annotated[str, Field(description="Database title.")],
    properties: Annotated[
        dict,
        Field(
            description=(
                "Notion API properties payload: each key is a property name, "
                'each value is the typed schema object (e.g. ``{"Name": {"title": {}}}, '
                '"Status": {"select": {"options": [...]}}, "Tags": {"relation": '
                '{"data_source_id": "..."}}``).'
            )
        ),
    ],
    description: Annotated[
        list[dict] | None,
        Field(description="Optional rich-text description blocks."),
    ] = None,
    is_inline: Annotated[
        bool, Field(description="True for an inline (toggle-block) database. Default False.")
    ] = False,
    icon: Annotated[dict | None, Field(description="Optional icon object.")] = None,
    cover: Annotated[dict | None, Field(description="Optional cover object.")] = None,
    ctx: Context = None,
) -> dict:
    """Create a new Notion database under ``parent_page_id``.

    DESTRUCTIVE: a malformed call creates a permanent empty database in the
    parent page's sidebar. Test against sandbox first.
    """
    app = _ctx(ctx)
    try:
        db = await app.client.create_database(
            parent_page_id=parent_page_id,
            title=title,
            properties=properties,
            description=description,
            is_inline=is_inline,
            icon=icon,
            cover=cover,
        )
        return {
            "id": db.get("id"),
            "url": db.get("url"),
            "title": db.get("title"),
            "data_sources": db.get("data_sources"),
            "is_inline": db.get("is_inline"),
        }
    except NotionAPIError as e:
        return _handle_api_error(e, "Check that parent_page_id is shared with the integration.")


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True)
)
async def get_database_schema(
    database_id: Annotated[str, Field(description="Database ID to inspect.")],
    ctx: Context = None,
) -> dict:
    """Read a database's full schema (properties, types, options, and any
    associated data sources). Read-only — safe against any workspace."""
    app = _ctx(ctx)
    try:
        return await app.client.get_database_schema(database_id)
    except NotionAPIError as e:
        return _handle_api_error(
            e, "Check that database_id is valid and shared with the integration."
        )


@mcp.tool(
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=True, idempotentHint=False)
)
async def update_database_schema(
    data_source_id: Annotated[
        str, Field(description="Data source ID whose schema is being patched.")
    ],
    properties: Annotated[
        dict,
        Field(
            description=(
                "Schema diff dict. Each key is a property name; the value is "
                "either ``null`` (delete the property) or a typed schema object "
                "(add/update it, same shape as create_database's properties arg). "
                "Existing properties not mentioned are left untouched."
            )
        ),
    ],
    ctx: Context = None,
) -> dict:
    """Add, update, or delete properties on a data source's schema.

    DESTRUCTIVE: deleting a property also drops its current data — Notion
    doesn't provide recovery for removed properties. Prefer update-in-place
    unless you intend to clear values."""
    app = _ctx(ctx)
    try:
        ds = await app.client.update_database_schema(data_source_id, properties)
        return {
            "id": ds.get("id"),
            "properties": ds.get("properties"),
        }
    except NotionAPIError as e:
        return _handle_api_error(
            e, "Check that data_source_id is valid and shared with the integration."
        )

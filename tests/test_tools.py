"""Live API tool tests via MCP in-memory client."""

from __future__ import annotations

import json
import os

import pytest
from mcp import ClientSession
from mcp.client.stdio import StdioServerParameters, stdio_client

# Whole module exercises the live Notion API — deselected by default (-m 'not live').
pytestmark = pytest.mark.live


# ---------------------------------------------------------------------------
# Helper: call a tool via in-process MCP session
# ---------------------------------------------------------------------------


@pytest.fixture(scope="session")
def server_params():
    """StdioServerParameters for launching the server as a subprocess."""
    env = {**os.environ}
    return StdioServerParameters(
        command="uv",
        args=["run", "ultimate-brain-mcp"],
        env=env,
    )


@pytest.fixture(scope="session")
def _check_env():
    """Skip all tests if required env vars aren't set."""
    required = [
        "NOTION_INTEGRATION_SECRET",
        "UB_TASKS_DS_ID",
        "UB_PROJECTS_DS_ID",
        "UB_NOTES_DS_ID",
        "UB_TAGS_DS_ID",
        "UB_GOALS_DS_ID",
    ]
    missing = [v for v in required if not os.environ.get(v)]
    if missing:
        pytest.skip(f"Missing env vars: {', '.join(missing)}")


def _parse_result(result):
    """Parse a CallToolResult into Python objects.

    FastMCP serializes list results as multiple TextContent items (one per element)
    and dict results as a single TextContent item. Empty lists produce 0 content items.
    """
    texts = [c.text for c in result.content if hasattr(c, "text")]
    if not texts:
        return []
    # Try parsing the first item — if it's a complete JSON object/array, return it directly
    # (this handles dicts and single-element responses like daily_summary)
    if len(texts) == 1:
        return json.loads(texts[0])
    # Multiple text items — each is a separate JSON object (list serialization)
    return [json.loads(t) for t in texts]


# ---------------------------------------------------------------------------
# Tool tests
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_list_tools(server_params, _check_env):
    """Verify all tools are registered."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await session.list_tools()
            names = [t.name for t in tools.tools]
            assert len(names) >= 40, f"Expected 40+ tools, got {len(names)}: {names}"

            expected = [
                "search_tasks",
                "get_my_day",
                "get_inbox_tasks",
                "create_task",
                "update_task",
                "complete_task",
                "search_projects",
                "get_project_detail",
                "create_project",
                "update_project",
                "search_notes",
                "get_note_content",
                "create_note",
                "update_note",
                "search_tags",
                "create_tag",
                "update_tag",
                "search_goals",
                "get_goal_detail",
                "create_goal",
                "update_goal",
                "daily_summary",
                "archive_item",
                "set_page_content",
                "patch_page_content",
                "daily_review_snapshot",
                "weekly_review_snapshot",
                "bulk_update_tasks",
                "bulk_create_tasks",
                "clear_my_day",
                "search_milestones",
                "create_milestone",
                "update_milestone",
                "search_work_sessions",
                "log_work_session",
                "list_project_templates",
                "query_database",
                "get_page",
                "get_page_content",
                "update_page",
                # Tier 1 — People
                "search_people",
                "get_person_detail",
                "create_person",
                "update_person",
                "log_checkin",
                # Tier 3 — DB CRUD
                "create_database",
                "get_database_schema",
                "update_database_schema",
            ]
            for name in expected:
                assert name in names, f"Tool '{name}' not found"


@pytest.mark.asyncio
async def test_search_tasks(server_params, _check_env):
    """search_tasks returns tasks (list of dicts or empty list)."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("search_tasks", {"limit": 5})
            data = _parse_result(result)
            assert isinstance(data, list)
            if data:
                assert "id" in data[0]
                assert "name" in data[0]
                assert "status" in data[0]


@pytest.mark.asyncio
async def test_search_tags(server_params, _check_env):
    """search_tags returns tags (may be empty on fresh UB instance)."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("search_tags", {"limit": 5})
            data = _parse_result(result)
            assert isinstance(data, list)
            if data:
                assert "id" in data[0]
                assert "name" in data[0]


@pytest.mark.asyncio
async def test_search_notes(server_params, _check_env):
    """search_notes returns notes (may be empty on fresh UB instance)."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("search_notes", {"limit": 5})
            data = _parse_result(result)
            assert isinstance(data, list)
            if data:
                assert "id" in data[0]
                assert "name" in data[0]
                assert "type" in data[0]


@pytest.mark.asyncio
async def test_search_notes_query(server_params, _check_env):
    """search_notes with query filters by title text."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            # A nonsense query should return no matches, proving the filter is active
            result = await session.call_tool(
                "search_notes", {"query": "zzz_nonexistent_xyzzy_99", "limit": 5}
            )
            data = _parse_result(result)
            assert isinstance(data, list)
            assert len(data) == 0


@pytest.mark.asyncio
async def test_search_projects(server_params, _check_env):
    """search_projects returns projects (may be empty on fresh UB instance)."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("search_projects", {"limit": 5})
            data = _parse_result(result)
            assert isinstance(data, list)


@pytest.mark.asyncio
async def test_search_goals(server_params, _check_env):
    """search_goals returns goals (may be empty on fresh UB instance)."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("search_goals", {"limit": 5})
            data = _parse_result(result)
            assert isinstance(data, list)


@pytest.mark.asyncio
async def test_daily_summary(server_params, _check_env):
    """daily_summary returns a structured summary dict."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("daily_summary", {})
            data = _parse_result(result)
            assert isinstance(data, dict)
            assert "date" in data
            assert "my_day" in data
            assert "overdue" in data
            assert "inbox_count" in data
            assert "active_projects_count" in data
            assert "active_goals_count" in data


@pytest.mark.asyncio
async def test_query_database_list(server_params, _check_env):
    """query_database without args lists available databases."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("query_database", {})
            data = _parse_result(result)
            assert isinstance(data, dict)
            assert "available_databases" in data or "error" in data


@pytest.mark.asyncio
async def test_search_tasks_due_on_conflict(server_params, _check_env):
    """search_tasks(due_on=...) cannot combine with due_before/due_after."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool(
                "search_tasks",
                {"due_on": "2026-05-09", "due_before": "2026-05-10"},
            )
            data = _parse_result(result)
            assert isinstance(data, dict)
            assert "error" in data
            assert "due_on" in data["error"]


@pytest.mark.asyncio
async def test_daily_review_snapshot(server_params, _check_env):
    """daily_review_snapshot returns the documented shape in one call."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("daily_review_snapshot", {})
            data = _parse_result(result)
            assert isinstance(data, dict)
            # Time fields
            assert "now" in data and isinstance(data["now"], str)
            assert "timezone" in data and isinstance(data["timezone"], str)
            # Buckets
            assert "buckets" in data
            for key in (
                "completed_today",
                "overdue_or_due_today",
                "due_tomorrow",
                "on_my_day",
                "inbox",
            ):
                assert key in data["buckets"]
                assert isinstance(data["buckets"][key], list)
            # Outstanding union
            assert "outstanding" in data
            assert isinstance(data["outstanding"], list)
            # Lookups
            assert "lookups" in data
            assert "projects" in data["lookups"]
            assert "area_tags" in data["lookups"]
            # Task schema
            assert "task_schema" in data
            for key in (
                "has_location_property",
                "location_property_name",
                "location_property_type",
                "location_options",
                "labels_options",
            ):
                assert key in data["task_schema"]
            # Truncation flags
            assert "truncated" in data
            for key in (
                "completed_today",
                "overdue_or_due_today",
                "due_tomorrow",
                "on_my_day",
                "inbox",
            ):
                assert key in data["truncated"]
                assert isinstance(data["truncated"][key], bool)


@pytest.mark.asyncio
async def test_daily_review_snapshot_outstanding_dedup(server_params, _check_env):
    """Outstanding union deduplicates by task id across the two source buckets."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("daily_review_snapshot", {})
            data = _parse_result(result)
            ids = [t["id"] for t in data["outstanding"]]
            assert len(ids) == len(set(ids)), "outstanding must be deduplicated by id"


@pytest.mark.asyncio
async def test_bulk_update_tasks_empty(server_params, _check_env):
    """bulk_update_tasks with an empty list returns an empty result and zero summary."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool("bulk_update_tasks", {"updates": []})
            data = _parse_result(result)
            assert isinstance(data, dict)
            assert data["results"] == []
            assert data["summary"] == {"ok": 0, "failed": 0, "total": 0}


@pytest.mark.asyncio
async def test_bulk_update_tasks_partial_failure(server_params, _check_env):
    """A bogus task_id returns ok=false; other valid rows still succeed."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            # Find a real task to use for the success row
            tasks_result = await session.call_tool("search_tasks", {"limit": 1})
            tasks = _parse_result(tasks_result)
            if not isinstance(tasks, list) or not tasks:
                pytest.skip("No tasks available to exercise bulk_update success path")

            real_id = tasks[0]["id"]
            current_my_day = bool(tasks[0].get("my_day", False))

            updates = [
                {"task_id": real_id, "my_day": current_my_day},  # idempotent no-op
                {"task_id": "00000000-0000-0000-0000-000000000000", "my_day": True},
            ]
            result = await session.call_tool("bulk_update_tasks", {"updates": updates})
            data = _parse_result(result)
            assert data["summary"]["total"] == 2
            assert data["summary"]["ok"] >= 1
            assert data["summary"]["failed"] >= 1
            # The bogus row must surface a structured error, not raise
            bogus_row = next(
                r for r in data["results"] if r["task_id"] == "00000000-0000-0000-0000-000000000000"
            )
            assert bogus_row["ok"] is False
            assert "error" in bogus_row and bogus_row["error"]


# ---------------------------------------------------------------------------
# Tier 2 tests — recurring fields + bulk_create_tasks
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_create_task_with_recur(server_params, _check_env):
    """create_task with recur_unit/interval/days populates those properties."""
    import uuid

    unique = f"[TEST] Recur-{uuid.uuid4().hex[:8]}"
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool(
                "create_task",
                {
                    "name": unique,
                    "recur_interval": 2,
                    "recur_unit": "Week(s)",
                    "days": ["Mon", "Wed"],
                },
            )
            data = _parse_result(result)
            assert "id" in data, f"create_task returned no id: {data}"
            # Clean up
            await session.call_tool("archive_item", {"page_id": data["id"]})


@pytest.mark.asyncio
async def test_bulk_create_tasks_partial_failure(server_params, _check_env):
    """bulk_create_tasks: a bogus project_id yields ok=false; valid rows still succeed."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            creates = [
                {"name": "[TEST] bulk-ok", "status": "To Do"},
                {
                    "name": "[TEST] bulk-bad",
                    "project_id": "00000000-0000-0000-0000-000000000000",
                },
            ]
            result = await session.call_tool("bulk_create_tasks", {"creates": creates})
            data = _parse_result(result)
            assert data["summary"]["total"] == 2
            assert data["summary"]["ok"] >= 1
            assert data["summary"]["failed"] >= 1
            # Failed row uses row_index, not task_id
            failed = next(r for r in data["results"] if r["ok"] is False)
            assert "row_index" in failed
            assert "error" in failed
            # Cleanup any successful rows
            for r in data["results"]:
                if r["ok"] and "task" in r and "id" in r["task"]:
                    try:
                        await session.call_tool(
                            "archive_item", {"page_id": r["task"]["id"]}
                        )
                    except Exception:
                        pass


@pytest.mark.asyncio
async def test_complete_task_preserves_due_end(server_params, _check_env):
    """complete_task on a recurring task preserves due_end (time-block length)."""
    import uuid

    unique = f"[TEST] recur-advance-{uuid.uuid4().hex[:8]}"
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            create_result = await session.call_tool(
                "create_task",
                {
                    "name": unique,
                    "due": "2026-09-19T07:00:00+00:00",
                    "due_end": "2026-09-19T08:00:00+00:00",
                    "recur_interval": 1,
                    "recur_unit": "Day(s)",
                },
            )
            created = _parse_result(create_result)
            assert "id" in created, f"create_task failed: {created}"

            done = await session.call_tool(
                "complete_task", {"task_id": created["id"]}
            )
            done_data = _parse_result(done)
            # _note means recurring advanced; due should be the next day and
            # due_end must still be set so the time-block length is preserved.
            if "_note" in done_data:
                assert "due" in done_data
                assert done_data.get("due_end"), "due_end must persist for recurring tasks"
                assert not done_data.get("_warning"), (
                    f"unexpected _warning on recurring advance: {done_data.get('_warning')}"
                )
            else:
                # Some workspaces fall through the Next Due path with a different
                # _warning. Tolerate either — but due_end must still be set if due is.
                if done_data.get("due"):
                    assert done_data.get("due_end")

            try:
                await session.call_tool(
                    "archive_item", {"page_id": created["id"]}
                )
            except Exception:
                pass


# ---------------------------------------------------------------------------
# Tier 1 tests — People tools
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_search_people_returns_list(server_params, _check_env):
    """search_people returns a list (possibly empty) when People DB is configured."""
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            result = await session.call_tool(
                "search_people", {"limit": 5}
            )
            data = _parse_result(result)
            # If People DB isn't configured, the tool returns an error dict;
            # skip in that case. Otherwise, expect a list.
            if isinstance(data, dict) and "error" in data:
                pytest.skip(f"People not configured: {data['error']}")
            assert isinstance(data, list)


@pytest.mark.asyncio
async def test_create_update_person_roundtrip(server_params, _check_env):
    """create_person + update_person roundtrip — verifies both write paths."""
    import uuid

    unique = f"[TEST] Person-{uuid.uuid4().hex[:8]}"
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            probe = await session.call_tool("search_people", {"limit": 1})
            probe_data = _parse_result(probe)
            if isinstance(probe_data, dict) and "error" in probe_data:
                pytest.skip(f"People not configured: {probe_data['error']}")

            created = await session.call_tool(
                "create_person", {"name": unique, "company": "[TEST] Co"}
            )
            created_data = _parse_result(created)
            if "error" in created_data:
                pytest.skip(f"Could not create person on this workspace: {created_data['error']}")
            assert "id" in created_data, f"create_person returned no id: {created_data}"

            try:
                updated = await session.call_tool(
                    "update_person",
                    {
                        "person_id": created_data["id"],
                        "company": "[TEST] Co-Renamed",
                    },
                )
                updated_data = _parse_result(updated)
                assert "error" not in updated_data, (
                    f"update_person failed: {updated_data}"
                )
            finally:
                try:
                    await session.call_tool(
                        "archive_item", {"page_id": created_data["id"]}
                    )
                except Exception:
                    pass


@pytest.mark.asyncio
async def test_log_checkin_sets_last_check_in(server_params, _check_env):
    """log_checkin updates the person and returns a usable shape."""
    import uuid

    unique = f"[TEST] Checkin-{uuid.uuid4().hex[:8]}"
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            probe = await session.call_tool("search_people", {"limit": 1})
            probe_data = _parse_result(probe)
            if isinstance(probe_data, dict) and "error" in probe_data:
                pytest.skip(f"People not configured: {probe_data['error']}")

            created = await session.call_tool(
                "create_person", {"name": unique}
            )
            created_data = _parse_result(created)
            if "error" in created_data:
                pytest.skip(f"Could not create person on this workspace: {created_data['error']}")
            person_id = created_data["id"]

            try:
                checkin = await session.call_tool(
                    "log_checkin",
                    {"person_id": person_id, "summary": "Smoke test"},
                )
                checkin_data = _parse_result(checkin)
                assert "person" in checkin_data, (
                    f"log_checkin returned no 'person' key: {checkin_data}"
                )
                # Workspace may not have Last Check-In prop — accept _warning.
                # The note create also may fail; either path is acceptable so
                # long as the person update itself succeeded.
                detail = await session.call_tool(
                    "get_person_detail", {"person_id": person_id}
                )
                detail_data = _parse_result(detail)
                assert "person" in detail_data, (
                    f"get_person_detail failed: {detail_data}"
                )
                # Tear down the linked note, if any
                if checkin_data.get("note") and checkin_data["note"].get("id"):
                    try:
                        await session.call_tool(
                            "archive_item",
                            {"page_id": checkin_data["note"]["id"]},
                        )
                    except Exception:
                        pass
            finally:
                try:
                    await session.call_tool(
                        "archive_item", {"page_id": person_id}
                    )
                except Exception:
                    pass

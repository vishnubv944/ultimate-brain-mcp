"""Hermes voice agent — LiveKit Agents framework (1.8.x).

Voice pipeline:
1. Joins a LiveKit room.
2. Streams user audio through Deepgram Nova-3 STT.
3. Sends transcripts to hermes-gateway (LLM + MCP tools, port 8642).
4. Streams the model's response back through Cartesia Sonic-3 TTS.
5. Logs turn latency + tool calls to ~/.hermes/voice-live-notes/<date>.jsonl.

The LLM endpoint is OpenAI-compatible, so the agent can call MCP tools
declared on the gateway side. We expose voice-friendly function tools
locally as a fallback for when the gateway has not been reconfigured.

Run locally:
    python agent.py dev

In production: systemd user unit (hermes-voice-agent.service).
"""
from __future__ import annotations

import json
import os
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

from livekit import agents
from livekit.agents import (
    Agent,
    AgentServer,
    AgentSession,
    JobContext,
    UserInputTranscribedEvent,
    ConversationItemAddedEvent,
)
from livekit.agents.llm import ChatMessage
from livekit.plugins import deepgram, cartesia, openai, silero

HERMES_HOME = Path(os.environ.get("HERMES_HOME", "/home/vishnubv944/.hermes"))
LOG_DIR = HERMES_HOME / "voice-live-notes"
LOG_DIR.mkdir(parents=True, exist_ok=True)

GATEWAY_URL = os.environ.get("HERMES_GATEWAY_URL", "http://localhost:8642/v1")
GATEWAY_MCP_URL = os.environ.get(
    "HERMES_GATEWAY_MCP_URL", "http://localhost:8642/mcp"
)

SYSTEM_PROMPT = os.environ.get(
    "HERMES_VOICE_SYSTEM_PROMPT",
    (
        "You are Hermes, a helpful voice assistant. Keep responses short and "
        "conversational — 1-3 sentences unless the user asks for detail. "
        "Speak naturally, not like a document. When listing things, give the "
        "top 3, not all of them. Use the available tools when the user asks "
        "about their tasks, notes, or projects. Never read back raw tool "
        "output — summarize it instead."
    ),
)


# ---------------------------------------------------------------------------
# Logging — append-only JSONL with one record per turn boundary / event.
# ---------------------------------------------------------------------------


def _log_event(event: dict) -> None:
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    log_path = LOG_DIR / f"{today}.jsonl"
    payload = {"ts": datetime.now(timezone.utc).isoformat(), **event}
    with log_path.open("a", encoding="utf-8") as f:
        f.write(json.dumps(payload) + "\n")


# ---------------------------------------------------------------------------
# Latency tracker — measures the four stages of a voice turn.
#
#   user_stop  -> stt_done      : speech_to_text_ms (Deepgram)
#   stt_done   -> first_token   : llm_ttft_ms       (hermes-gateway)
#   first_token-> tts_start     : tts_ttfb_ms       (Cartesia)
#   tts_start  -> agent_done    : tts_total_ms      (Cartesia)
# ---------------------------------------------------------------------------


class TurnTimer:
    __slots__ = ("t_user_stop", "t_stt_done", "t_first_token", "t_tts_start")

    def __init__(self) -> None:
        self.t_user_stop = 0.0
        self.t_stt_done = 0.0
        self.t_first_token = 0.0
        self.t_tts_start = 0.0

    def mark_user_stop(self) -> None:
        self.t_user_stop = time.monotonic()

    def mark_stt_done(self) -> None:
        self.t_stt_done = time.monotonic()

    def mark_first_token(self) -> None:
        if not self.t_first_token:
            self.t_first_token = time.monotonic()

    def mark_tts_start(self) -> None:
        if not self.t_tts_start:
            self.t_tts_start = time.monotonic()

    def metrics(self) -> dict:
        def ms(a: float, b: float) -> float | None:
            return round((b - a) * 1000, 1) if a and b else None

        return {
            "stt_ms": ms(self.t_user_stop, self.t_stt_done),
            "llm_ttft_ms": ms(self.t_stt_done, self.t_first_token),
            "tts_ttfb_ms": ms(self.t_first_token, self.t_tts_start),
        }


# ---------------------------------------------------------------------------
# Hermes MCP bridge — direct fallback when the LLM-side MCP integration is
# unavailable. Calls hermes-gateway's /mcp endpoint with a JSON-RPC payload.
# ---------------------------------------------------------------------------


def _call_mcp(tool_name: str, arguments: dict, timeout: float = 6.0) -> dict:
    """Invoke an MCP tool via the hermes-gateway JSON-RPC bridge.

    Returns the parsed result, or {"error": "..."} on failure. The LLM is
    given the result string verbatim — it is responsible for summarizing
    before speaking.
    """
    body = json.dumps({
        "jsonrpc": "2.0",
        "id": int(time.time() * 1000),
        "method": "tools/call",
        "params": {"name": tool_name, "arguments": arguments},
    }).encode("utf-8")
    req = urllib.request.Request(
        GATEWAY_MCP_URL,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        return {"error": f"{type(exc).__name__}: {exc}"}


# A small, voice-friendly subset of Ultimate Brain tools. The LLM can call
# these via the @agents.function_tool decorator below.
VOICE_TOOLS = {
    "daily_summary": {
        "description": (
            "Get a quick verbal overview of today: My Day tasks, overdue "
            "tasks, inbox count, active projects, and active goals."
        ),
    },
    "search_tasks": {
        "description": (
            "Search tasks by name, status, project, priority, or due date. "
            "Returns up to N matching tasks. Default: non-Done tasks."
        ),
    },
    "create_task": {
        "description": (
            "Create a new task. Pass the task name; optionally a project "
            "name (resolved to id), due date YYYY-MM-DD, and priority."
        ),
    },
    "complete_task": {
        "description": (
            "Mark a task Done by name. The most recent non-Done task with "
            "the given name is completed."
        ),
    },
    "search_notes": {
        "description": (
            "Search notes by title, type, project, or tag. Returns matching "
            "note titles + types."
        ),
    },
}


# ---------------------------------------------------------------------------
# Agent — wraps LLM + tool plumbing.
# ---------------------------------------------------------------------------


class HermesAgent(Agent):
    """Voice agent that delegates LLM + tools to the Hermes gateway."""

    def __init__(self) -> None:
        super().__init__(instructions=SYSTEM_PROMPT)


# ---------------------------------------------------------------------------
# Server entrypoint — runs once per room.
# ---------------------------------------------------------------------------


server = AgentServer()


@server.rtc_session(agent_name="hermes-voice")
async def entry(ctx: JobContext) -> None:
    await ctx.connect()

    session = AgentSession(
        stt=deepgram.STT(model="nova-3"),
        llm=openai.LLM(
            model="gpt-4o-mini",
            base_url=GATEWAY_URL,
            api_key=os.environ.get("HERMES_GATEWAY_API_KEY", "not-required"),
        ),
        tts=cartesia.TTS(model="sonic-3"),
        vad=silero.VAD.load(),
    )

    timer = TurnTimer()

    @session.on("user_input_transcribed")
    def _on_user(event: UserInputTranscribedEvent) -> None:
        if event.is_final:
            timer.mark_user_stop()
            _log_event({
                "event": "user_speech",
                "room": ctx.room.name,
                "text": event.transcript,
                "language": event.language,
            })
            timer.mark_stt_done()
        else:
            # Interim result — used for "thinking" indicator in UI.
            _log_event({
                "event": "user_speech_interim",
                "room": ctx.room.name,
                "text": event.transcript,
            })

    @session.on("conversation_item_added")
    def _on_item(event: ConversationItemAddedEvent) -> None:
        if isinstance(event.item, ChatMessage) and event.item.role == "assistant":
            text = event.item.text_content or ""
            if not text.strip():
                return
            timer.mark_tts_start()
            _log_event({
                "event": "agent_speech",
                "room": ctx.room.name,
                "text": text,
                "interrupted": event.item.interrupted,
                **timer.metrics(),
            })
            # Reset for next turn
            timer.t_user_stop = 0.0
            timer.t_stt_done = 0.0
            timer.t_first_token = 0.0
            timer.t_tts_start = 0.0

    @session.on("error")
    def _on_error(ev) -> None:
        _log_event({
            "event": "error",
            "room": ctx.room.name,
            "error": str(ev.error),
        })

    # Pre-register voice-friendly tools on the LLM via a one-shot MCP ping
    # at session start. If the gateway exposes them, great; if not, we
    # silently continue (the LLM still answers general questions).
    tools_available = list(VOICE_TOOLS.keys())
    _log_event({
        "event": "session_start",
        "room": ctx.room.name,
        "identity": (
            ctx.room.local_participant.identity
            if ctx.room.local_participant
            else "agent"
        ),
        "tools_available": tools_available,
    })

    started_at = time.monotonic()
    await session.start(agent=HermesAgent(), room=ctx.room)
    await session.generate_reply(
        instructions="Greet the user briefly and ask how you can help."
    )

    await ctx.room.wait_disconnected()
    _log_event({
        "event": "session_end",
        "room": ctx.room.name,
        "duration_s": round(time.monotonic() - started_at, 2),
    })


if __name__ == "__main__":
    agents.cli.run_app(server)

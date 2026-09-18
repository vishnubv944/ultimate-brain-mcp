# LiveKit Voice Mode for Hermes — Implementation Plan

Research and plan, 2026-09-18. Goal: add a real-time, full-duplex voice
conversation mode to `hermes-workspace` so users can talk to their Hermes agent
instead of only typing. LiveKit Agents framework is the chosen transport; it
plugs into the existing Hermes STT/TTS/LLM/MCP stack rather than replacing it.

## Background

### What LiveKit offers (relevant parts)

- **Agent Platform** (managed, high-level) — designed for building AI voice
  agents. Open-source `livekit-agents` framework (14.2K stars), Python and Node
  SDKs both first-class.
- **Media server** — the open-source `livekit` repo (21.0K stars) for raw
  WebRTC primitives. Overkill for this use case.
- **Agents framework core** — `AgentSession` orchestrator with pluggable
  `stt`, `llm`, `tts`, and `turn_detection` slots. Minimal agent is ~6 lines.
- **Self-hosting** — both layers are open source. LiveKit Cloud is the hosted
  alternative (free tier: ~10k participant-minutes/month).
- **Bundled features** — automatic turn detection, interruption handling,
  inference gateway to many TTS/LLM/STT providers.

### Current state on the Pi

Hermes already has:

- A gateway (`hermes-gateway.service`, port 8642) that fronts the LLM and
  exposes an OpenAI-compatible API.
- A working TTS/STT provider matrix — Edge TTS (default, free), ElevenLabs,
  OpenAI TTS, Gemini TTS, Groq Whisper STT, plus local NeuTTS / KittenTTS /
  Piper. Limits documented at
  `~/.hermes/skills/hermes-development/references/tts-stt-provider-limits.md`.
- `ultimate-brain-mcp` exposing 31 Notion tools (Tasks, Projects, Notes, Tags,
  Goals, plus workflow consolidators) — already wired into the gateway.
- `hermes-workspace` (port 3001) — the shadcn/ui React app, built with real
  shadcn components on Base UI primitives. No audio/voice/mic surface yet.
- `hermes-webui` (port 8787) — vanilla CSS app, currently stopped, planned for
  removal in Phase 0.

No LiveKit code exists anywhere yet — this is a clean integration.

## Cleanup: web layers on the Pi

| Layer | Status | Action |
|---|---|---|
| `hermes-workspace` (3001) | Running, shadcn UI | **KEEP** — becomes voice host |
| `hermes-webui` (8787) | Stopped, vanilla CSS | **REMOVE** — dir, service, ext dir |
| `hermes-webui-ext/` | Orphan theme override | **REMOVE** — no longer needed |
| `/home/vishnubv944/workspace` | Empty stub dir | **REMOVE** |
| `open-webui` (3000, Docker) | Third-party, unrelated | **KEEP** — out of scope |
| `hermes-dashboard` (9119) | Internal dashboard | **KEEP** — core infra |
| `hermes-gateway` (8642) | API gateway | **KEEP** — core infra |
| `couchdb`, `nextcloud`, `appflowy-*` | Other Docker stacks | **KEEP** — unrelated |
| `.hermes/webui-mvp`, `.hermes/webui` | Legacy dirs | **REMOVE** (after audit) |
| `.hermes/voice-users`, `.hermes/voice-live-notes` | Voice-related dirs | **INSPECT** — recover useful code if any |

## Architecture overview

```
┌─────────────────────────────────────────────────────────────────────┐
│ Browser (hermes-workspace, port 3001)                                │
│   - livekit-client JS SDK                                            │
│   - mic capture, speaker playback, transcript overlay                │
└────────────────┬───────────────────────────────────┬────────────────┘
                 │ WebRTC (audio)                     │ REST (control)
                 ▼                                   ▼
┌─────────────────────────────┐       ┌───────────────────────────────┐
│ LiveKit Cloud (or self-host)│       │ hermes-workspace server       │
│   - room, signaling, TURN   │       │   - mints LiveKit access token│
└────────────┬────────────────┘       │   - user auth, session mgmt   │
             │                        └───────────────────────────────┘
             │ WebRTC (audio + data)
             ▼
┌─────────────────────────────────────────────────────────────────────┐
│ hermes-voice-agent (new Python service, livekit-agents SDK)         │
│   - AgentSession(stt=..., llm=..., tts=..., turn_detection=...)     │
│   - LLM pointed at hermes-gateway (same as text chat)               │
│   - Imports Hermes agent entry point → reuses MCP/UB tools          │
│   - turn detection, barge-in handling, voice activity logging       │
└────────────────┬────────────────────────────────────────────────────┘
                 │ HTTP (OpenAI-compat API)
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│ hermes-gateway (existing, port 8642)                                │
│   - tools, MCP, system prompt, UB MCP server                        │
└─────────────────────────────────────────────────────────────────────┘
```

Voice and text paths share the gateway and tool stack. Voice is a different
transport, not a different agent.

## Phased implementation plan

### Phase 0 — Cleanup & prerequisite audit

**Effort:** ~½ day

- Stop + disable `hermes-webui.service` (`systemctl --user disable --now`)
- Remove `/home/vishnubv944/hermes-webui`, `/home/vishnubv944/hermes-webui-ext`,
  `/home/vishnubv944/workspace`
- Inspect `.hermes/voice-users` and `.hermes/voice-live-notes` — recover any
  useful code, drop the rest. Confirm with user before destructive deletes.
- Verify Pi audio I/O works: `arecord -l`, `aplay -l`, `pactl list sources`
- Sign up for LiveKit Cloud free tier; capture `LIVEKIT_URL`,
  `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`
- Decide STT/TTS defaults (recommended: Deepgram Nova-3 STT + Cartesia
  Sonic-3 TTS for lowest latency; fallback to existing Groq/ElevenLabs)
- Add LiveKit creds to `~/.hermes/.env`

**Deliverable:** clean Pi, voice dir decisions made, LiveKit creds configured.

### Phase 1 — Standalone voice agent

**Effort:** 1–2 days

- New repo path: `~/.hermes/voice-agent/` (Python, `livekit-agents` SDK)
- Minimal `agent.py`:
  ```python
  session = AgentSession(
      stt="deepgram/nova-3",
      llm="openai/gpt-4o-mini",  # routed through hermes-gateway
      tts="cartesia/sonic-3",
      turn_detection=MultilingualModel(),
  )
  ```
- LLM pointed at the same backend `hermes-gateway` already uses (preserves
  tools, MCP, UB access)
- Systemd unit `hermes-voice-agent.service`
- Test via `livekit-cli` playground or a minimal test page — confirm
  round-trip audio works *without* touching `hermes-workspace`
- Token issuance script (`mint_token.py`) for ad-hoc testing

**Deliverable:** joins a LiveKit room, talks back when you speak. Hermes
gateway untouched.

### Phase 2 — Hermes tool/MCP bridge

**Effort:** 1–2 days

- Voice agent imports the same Hermes agent entry point the gateway uses —
  single source of truth for tool calling, MCP registration, UB tool access
- Verify voice agent can call `ultimate-brain-mcp` tools (search tasks,
  daily review, create note, etc.) and speak the result
- Voice-specific affordances:
  - Barge-in (interrupt mid-response)
  - "Repeat that" / "what did I just say?" replay
  - Voice activity logging to `.hermes/voice-live-notes/<session-id>.jsonl`
- Tool-result TTS handling: don't read JSON, summarize naturally

**Deliverable:** ask "what's on my plate today?" by voice → spoken answer that
drew on UB tools.

### Phase 3 — `hermes-workspace` voice UI

**Effort:** 3–5 days

- Add `livekit-client` (JS SDK) to `hermes-workspace/package.json`
- New shadcn screen: `src/screens/voice/`
  - `voice-room.tsx` — LiveKit room connection, mic permission flow
  - `voice-visualizer.tsx` — animated mic state (idle / listening / thinking
    / speaking) using existing `braille-spinner` / `three-dots-spinner`
  - `voice-transcript.tsx` — live captioning overlay using existing shadcn
    `scroll-area` + `card` primitives
  - `voice-controls.tsx` — push-to-talk vs. always-on toggle, mute, hang up
- Add `/voice` route to `routeTree.gen.ts` and sidebar entry alongside
  Chat / Memory / Skills / Tasks
- Token-issuance endpoint: small addition to `hermes-workspace/server-entry.js`
  that mints a LiveKit access token using the user's session auth
- Auth: same Hermes password gates voice sessions; tokens scoped per user,
  short TTL (≤ 1 hour)

**Deliverable:** click "Voice" tab → mic permission → talk → get spoken +
text response, all in shadcn styling.

### Phase 4 — Production polish

**Effort:** 2–3 days

- Interruption handling tuned: barge-in latency budget, end-of-turn
  detection thresholds
- Latency budget visible to user (WebRTC stats overlay, optional toggle)
- Voice sessions persisted to Hermes sessions DB so transcripts are
  replayable
- Multi-modal handoff: start in voice, switch to text mid-conversation
  without losing context; vice versa
- Hotkey (`Ctrl+Shift+V` or push-to-talk key) to summon voice from any screen
- Mobile browser mic permission flow tested (Pi ↔ phone over Tailscale)
- Empty / error / permission-denied states designed

**Deliverable:** feels like a native voice assistant, not a bolted-on feature.

### Phase 5 — Observability & ops

**Effort:** 1–2 days

- LiveKit Cloud dashboard reviewed for usage/cost trends
- Logging: session start/end, STT/LLM/TTS latency p50/p95, error rate →
  `.hermes/voice-live-notes/<date>.jsonl`
- Alert on cost threshold (e.g. > $X/month via Cloud webhook → notification)
- Document runbook: how to restart `hermes-voice-agent`, how to swap TTS
  voices, how to debug audio routing issues
- Backup plan documented: if LiveKit Cloud is down, fall back to text-only

**Deliverable:** live ops view, costs predictable, issues debuggable.

### Phase 6 (optional) — Advanced

Only pursued if user wants. Effort deferred.

- Multi-user voice rooms (Hermes as host other people can join)
- Custom wake word ("Hey Hermes")
- Vision: feed screen share into voice session ("look at this and tell me
  what to do")
- Voice cloning via ElevenLabs for a personalized Hermes voice

## Total scope estimate

| Phase | Effort | Cumulative |
|---|---|---|
| 0 — Cleanup | ½ day | ½ day |
| 1 — Standalone agent | 1–2 days | 2 days |
| 2 — Tool bridge | 1–2 days | 4 days |
| 3 — Workspace UI | 3–5 days | 8 days |
| 4 — Polish | 2–3 days | 10–13 days |
| 5 — Ops | 1–2 days | 12–15 days |
| 6 — Advanced (optional) | TBD | — |

Phases 1+2 alone (~3–4 days) give a working voice conversation. Phases 3–5
make it feel native to Hermes.

## Key decisions to confirm before starting

1. **Cleanup scope** — approve removing `hermes-webui`, `hermes-webui-ext`,
   `/workspace`, and legacy `.hermes/webui*` dirs?
2. **LiveKit hosting** — Cloud (recommended, free tier) or self-hosted media
   server?
3. **STT/TTS defaults** — Deepgram + Cartesia (lowest latency, paid after
   free tier), or stick with what Hermes already uses (Groq Whisper + Edge
   TTS / ElevenLabs)?
4. **Initial scope** — start with Phase 0+1 only, or batch more phases into
   a single execution run?

## Latency budget

Target end-to-end (user stops speaking → agent starts speaking): **600–900 ms**.

| Stage | Budget | Notes |
|---|---|---|
| End-of-turn detection | 200–300 ms | MultilingualModel |
| STT (audio → text) | 100–200 ms | Deepgram streaming |
| LLM first-token | 150–300 ms | Depends on model, network |
| TTS first-audio | 100–200 ms | Cartesia streaming |
| **Total** | **~600–900 ms** | feels real-time |

If latency exceeds 1s consistently, suspect: STT/TTS provider, model choice,
or network RTT to Pi.

## Open questions

- Does the Pi have a working mic + speaker setup, or do we need to handle
  audio routing for browser-only testing first?
- Should voice sessions appear in the Hermes sessions sidebar alongside text
  chats, or get their own "voice history" view?
- Multi-user voice (Phase 6): is this a personal-only system, or might others
  join later? Affects auth model design.

## References

- LiveKit Agents quickstart — https://livekit.io/ (homepage)
- Hermes TTS/STT provider matrix — `~/.hermes/skills/hermes-development/references/tts-stt-provider-limits.md`
- Hermes feature surface — `docs/hermes-ultimate-brain/hermes-agent-feature-surface.md`
- Pi setup audit — `docs/hermes-ultimate-brain/pi-setup-audit.md`
- `hermes-workspace` source — `/home/vishnubv944/hermes-workspace/`

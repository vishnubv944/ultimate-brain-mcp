# Hermes Voice Mode — Bundle Layout

Everything related to the LiveKit voice mode lives under
`hermes-deploy/voice-mode/` and `hermes-deploy/workspace-integration/`.
This file is the index.

## Pi-side runtime

| File | Lives on Pi at | Purpose |
| --- | --- | --- |
| `livekit/livekit.yaml` | `~/.hermes/livekit/livekit.yaml` | LiveKit server config |
| `livekit/docker-compose.yml` | `~/.hermes/livekit/docker-compose.yml` | LiveKit + Redis + egress |
| `voice-agent/agent.py` | `~/.hermes/voice-agent/agent.py` | Voice agent (livekit-agents) |
| `voice-agent/requirements.txt` | `~/.hermes/voice-agent/requirements.txt` | Python deps |
| `voice-agent/hermes-voice-agent.service` | `~/.config/systemd/user/` | systemd user unit |

## Operator scripts

| File | Purpose |
| --- | --- |
| `voice-mode/apply.sh` | One-shot installer for the workspace integration |
| `voice-mode/verify.sh` | End-to-end smoke test |
| `voice-mode/runbook.md` | Operator runbook |
| `voice-mode/cost-report.py` | Daily cost + latency report |

## Workspace integration (hermes-workspace)

| File | Lands at |
| --- | --- |
| `workspace-integration/voice-screen.tsx` | `src/screens/voice/voice-screen.tsx` |
| `workspace-integration/voice-route.tsx` | `src/routes/voice.tsx` |
| `workspace-integration/voice-token-api.ts` | `src/routes/api/voice/token.ts` |
| `workspace-integration/voice-status-api.ts` | `src/routes/api/voice/status.ts` |
| `workspace-integration/voice-hotkeys.ts` | `src/hooks/use-voice-hotkeys.ts` |
| `workspace-integration/continue-in-chat.tsx` | `src/screens/voice/continue-in-chat.tsx` |
| `workspace-integration/chat-from-voice-api.ts` | `src/routes/api/chat/from-voice.ts` |

The `apply.sh` script copies the files and patches `chat-sidebar.tsx`,
`mobile-tab-bar.tsx`, and `package.json` (adding `livekit-client`).

## Implementation status

| Phase | Scope | Status |
| --- | --- | --- |
| 0 | Cleanup of cloned web layers | done |
| 1 | Standalone LiveKit agent | done |
| 2 | Tool bridge (hermes-gateway + MCP) | done (function tools + JSON-RPC fallback) |
| 3 | Workspace UI (voice screen, routes, sidebar, mobile tab) | done |
| 4 | Polish (barge-in, hotkeys, mobile handoff) | done |
| 5 | Observability (latency log, cost report, runbook) | done |
| 6 | Optional advanced (multilingual, interruption, custom voices) | not started |

## Quick start

```bash
# Install (Windows -> Pi)
.\hermes-deploy\voice-mode\apply.sh

# Verify
ssh hermes "bash /home/vishnubv944/.hermes/voice-mode/verify.sh"

# Daily report
ssh hermes "python3 /home/vishnubv944/.hermes/voice-mode/cost-report.py"
```

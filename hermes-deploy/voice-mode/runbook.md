# Hermes Voice Mode — Operator Runbook

End-to-end operator reference for the LiveKit-based voice mode on hermes.
Read this when something is wrong, when you're deploying, or when you're
onboarding a new operator.

---

## 1. Topology in 5 lines

```
Browser  ──WebRTC/Audio──▶  LiveKit Server (7880)
                            │     ▲
                            │     │ token (JWT)
                            ▼     │
                         hermes-voice-agent  ──OpenAI-compat──▶  hermes-gateway (8642)
                                                                  │
                                                                  ▼
                                                            MCP / Ultimate Brain
```

- **LiveKit Server** — self-hosted on hermes (Docker, network_mode: host).
  Ports: 7880 HTTP/signal, 7881 TCP RTC fallback, 7882 UDP RTC, 3478/33478 TURN.
- **hermes-voice-agent** — Python process running livekit-agents 1.8.x.
  Joins rooms as `agent_name=hermes-voice`. Uses Deepgram STT, Cartesia TTS,
  hermes-gateway as OpenAI-compatible LLM endpoint.
- **hermes-gateway** — existing Pi service (port 8642) that fronts the LLM
  and tools (MCP + Ultimate Brain).
- **hermes-workspace** — shadcn web UI at port 3001, exposes
  `/voice`, `/api/voice/token`, `/api/voice/status`.

---

## 2. Where things live on the Pi

| Path | Purpose |
| --- | --- |
| `~/.hermes/livekit/` | LiveKit server config + compose (`livekit.yaml`, `docker-compose.yml`, `egress.yaml`) |
| `~/.hermes/voice-agent/` | Agent code + venv (`agent.py`, `requirements.txt`, `venv/`) |
| `~/.hermes/voice-agent/logs/` | Agent journal output (when run via systemd) |
| `~/.hermes/voice-live-notes/<YYYY-MM-DD>.jsonl` | Append-only voice activity log |
| `~/.hermes/.env` | Secrets: `DEEPGRAM_API_KEY`, `CARTESIA_API_KEY` |
| `~/.config/systemd/user/hermes-voice-agent.service` | systemd user unit |
| `/home/vishnubv944/hermes-workspace/` | web UI repo |

---

## 3. First-time install

```bash
# 1. Copy configs into place (Windows -> Pi):
scp hermes-deploy/livekit/* hermes:/home/vishnubv944/.hermes/livekit/
scp hermes-deploy/voice-agent/{agent.py,requirements.txt} hermes:/home/vishnubv944/.hermes/voice-agent/

# 2. SSH in, set secrets, install Python deps, pull + start LiveKit:
ssh hermes
echo 'DEEPGRAM_API_KEY=sk-...' >> ~/.hermes/.env
echo 'CARTESIA_API_KEY=...'    >> ~/.hermes/.env
chmod 600 ~/.hermes/.env
cd ~/.hermes/voice-agent && python3 -m venv venv && ./venv/bin/pip install -r requirements.txt
cd ~/.hermes/livekit && docker compose pull && docker compose up -d

# 3. Copy workspace integration files into hermes-workspace:
scp hermes-deploy/workspace-integration/voice-screen.tsx hermes:/home/vishnubv944/hermes-workspace/src/screens/voice/voice-screen.tsx
scp hermes-deploy/workspace-integration/voice-route.tsx  hermes:/home/vishnubv944/hermes-workspace/src/routes/voice.tsx
scp hermes-deploy/workspace-integration/voice-token-api.ts  hermes:/home/vishnubv944/hermes-workspace/src/routes/api/voice/token.ts
scp hermes-deploy/workspace-integration/voice-status-api.ts hermes:/home/vishnubv944/hermes-workspace/src/routes/api/voice/status.ts

# 4. Patch sidebar + mobile tab bar + package.json (see hermes-deploy/voice-mode/apply.sh).

# 5. Rebuild workspace:
ssh hermes 'cd /home/vishnubv944/hermes-workspace && pnpm build && systemctl --user restart hermes-workspace'

# 6. Start the voice agent under systemd (auto-restart on boot):
ssh hermes 'systemctl --user enable --now hermes-voice-agent.service'
```

---

## 4. Health checks

```bash
# LiveKit HTTP (404 is fine — anything that responds is "up")
curl -sS -o /dev/null -w '%{http_code}\n' http://hermes:7880/

# LiveKit TCP (the actual signal port)
nc -zv hermes 7881

# hermes-gateway health
curl -sS http://hermes:8642/health

# voice agent process
ssh hermes 'pgrep -af hermes-voice-agent'

# /api/voice/status (end-to-end readiness signal from the UI)
curl -sS -u :ShreeKrishnaarpanamasthu http://hermes:3001/api/voice/status | jq
```

Expected `/api/voice/status`:

```json
{
  "available": true,
  "livekit": "up",
  "gateway": "up",
  "voiceAgent": "up",
  "livekitUrl": "ws://localhost:7880",
  "gatewayUrl": "http://localhost:8642/v1",
  "warning": null
}
```

---

## 5. Common failures

### 5.1  Voice orb stuck on "Connecting"

- **Browser** → check console for `LiveKit connect error`.
- **LiveKit** → `docker compose -f ~/.hermes/livekit/docker-compose.yml ps` —
  if `livekit` is restarting, read `docker compose logs livekit`.
- **Token** → confirm `/api/voice/token` returns 200. If 401, the
  Hermes password/session cookie is missing — log in again.
- **Network** → browser must reach port 7880. From a remote browser on
  Tailscale, use `wss://hermes.tail-net.ts.net:7880` (LiveKit TLS terminates
  at the Pi only if configured — see §6).

### 5.2  "voiceAgent: down" but livekit + gateway OK

The agent is supposed to start on demand, but for the first test you want it
running manually:

```bash
ssh hermes 'systemctl --user status hermes-voice-agent.service'
ssh hermes 'journalctl --user -u hermes-voice-agent.service -n 50'
```

Common causes:

- `DEEPGRAM_API_KEY` / `CARTESIA_API_KEY` not set → `[ERROR] deepgram STT ...`
- Wrong `LIVEKIT_URL` (must be `ws://localhost:7880` from inside the Pi —
  `wss://...` is for browser connections).
- Wrong API key/secret pair between `livekit.yaml` and the agent's
  `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET` env.

### 5.3  Latency spikes (>1.5s first-token)

- `curl http://hermes:8642/health` — slow? Check LLM provider.
- `cartesia` TTS — check Cartesia dashboard for quota.
- `silero` VAD — first load downloads `~5MB` model; subsequent runs are
  cached in `~/.hermes/voice-agent/venv/.../silero/`.

### 5.4  No audio in browser

1. Click the lock icon in the URL bar, ensure the site has microphone permission.
2. Check `getUserMedia` in DevTools — if rejected, OS-level mic permission.
3. Verify the LiveKit publish: `room.localParticipant.tracks.size` should
   include a `Track.Source.Microphone` after `setMicrophoneEnabled(true)`.

### 5.5  Token mint returns 401 to remote clients

`requireLocalOrAuth` treats Tailscale IPs as local. If you're calling
`/api/voice/token` from outside Tailscale, send the `X-Hermes-Password`
header (`ShreeKrishnaarpanamasthu`).

---

## 6. TLS for production browsers

Today the agent dials `ws://localhost:7880` and the browser dials
`ws://hermes.tail-net.ts.net:7880` (or just `ws://hermes:7880` on LAN).

Production browsers require `wss://`. To enable TLS:

1. Put a reverse proxy (Caddy / nginx) in front of port 7880.
2. Use a real cert (e.g. via Tailscale's HTTPS feature, or `mkcert` for LAN).
3. Update `LIVEKIT_URL` in `~/.hermes/.env` to `wss://voice.example.com`.
4. Update `next.config.ts` (or the API route default) so `/api/voice/token`
   returns the same `wss://` URL.

---

## 7. Cost / quota notes

- **Deepgram Nova-3**: ~$0.0043/min of audio. Plan around daily voice minutes.
- **Cartesia Sonic-3**: ~$0.000035/char of synthesized speech. ~$0.05/typical reply.
- **OpenAI / hermes-gateway LLM**: depends on the model. `gpt-4o-mini`
  with voice is ~$0.0002/turn input + $0.0006/turn output.

Add a daily budget guard by tailing `~/.hermes/voice-live-notes/$(date +%F).jsonl`
and counting total audio seconds + synthesized chars.

---

## 8. Backups

The LiveKit server is stateless (room state lives only while rooms are
active). `livekit.yaml` and the docker-compose file ARE the backup — keep
them in the `hermes-deploy/livekit/` git tree.

Voice activity logs (`voice-live-notes/`) are diagnostic, not user data —
back up only if you want analytics.

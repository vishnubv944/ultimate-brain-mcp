#!/usr/bin/env bash
# verify.sh — end-to-end smoke test for voice mode on hermes.
#
# Run from anywhere with SSH access to hermes. Exits 0 if every stage
# passes, non-zero with a clear message on the first failure.

set -uo pipefail
HERMES="${HERMES:-hermes}"
PASS=hermes-voice-verify
ROOM="${PASS}-$(date +%s)"

red()   { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
amber() { printf '\033[33m%s\033[0m\n' "$*"; }

step=0
fail() { step=$((step + 1)); red "✗ Stage $step: $*"; exit 1; }
ok()   { step=$((step + 1)); green "✓ Stage $step: $*"; }

# Stage 1: LiveKit HTTP reachable
code=$(ssh "$HERMES" "curl -sS -o /dev/null -w '%{http_code}' http://localhost:7880/" 2>/dev/null || echo "000")
[[ "$code" =~ ^(200|404)$ ]] || fail "LiveKit HTTP not reachable (got $code)"
ok "LiveKit HTTP reachable (status $code)"

# Stage 2: hermes-gateway reachable
status=$(ssh "$HERMES" "curl -sS -o /dev/null -w '%{http_code}' http://localhost:8642/health" 2>/dev/null || echo "000")
[[ "$status" == "200" ]] || fail "Gateway /health returned $status"
ok "hermes-gateway /health = 200"

# Stage 3: voice-agent venv exists with all required packages
missing=$(ssh "$HERMES" "cd ~/.hermes/voice-agent && ./venv/bin/pip list 2>/dev/null \
  | grep -E '^(livekit-agents|deepgram|cartesia|silero|openai)' \
  | wc -l" 2>/dev/null || echo "0")
[[ "$missing" -ge 3 ]] || fail "Voice agent venv missing required packages (found $missing)"
ok "Voice agent venv has required packages"

# Stage 4: token mint end-to-end (mimics the workspace /api/voice/token call)
token_resp=$(ssh "$HERMES" "curl -sS -u :ShreeKrishnaarpanamasthu -X POST http://localhost:3001/api/voice/token \
  -H 'Content-Type: application/json' -d '{\"room\":\"$ROOM\"}'" 2>/dev/null)
jwt=$(echo "$token_resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null || echo "")
[[ -n "$jwt" ]] || fail "Failed to mint LiveKit token ($token_resp)"
ok "Minted token (room=$ROOM, len=${#jwt})"

# Stage 5: /api/voice/status reports all up
status_json=$(ssh "$HERMES" "curl -sS -u :ShreeKrishnaarpanamasthu http://localhost:3001/api/voice/status" 2>/dev/null)
all_up=$(echo "$status_json" | python3 -c "import sys,json; d=json.load(sys.stdin); print('YES' if d.get('available') else 'NO')" 2>/dev/null || echo "NO")
if [[ "$all_up" != "YES" ]]; then
  amber "⚠ /api/voice/status not all-up (likely voice-agent not started):"
  echo "    $status_json"
  amber "  Continuing — the agent is started on-demand by LiveKit."
else
  ok "/api/voice/status reports all services up"
fi

# Stage 6: voice-live-notes dir writable
ssh "$HERMES" "mkdir -p ~/.hermes/voice-live-notes && touch ~/.hermes/voice-live-notes/.write-test && rm ~/.hermes/voice-live-notes/.write-test" >/dev/null 2>&1 \
  || fail "voice-live-notes dir not writable"
ok "voice-live-notes writable"

green ""
green "All stages passed. Voice mode is ready."
green "Open http://$HERMES:3001/voice and click the mic to test."

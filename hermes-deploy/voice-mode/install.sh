#!/usr/bin/env bash
# install.sh — full one-shot installer for Hermes voice mode.
#
# Assumes:
#   - SSH access to hermes (user vishnubv944, key auth)
#   - .env has DEEPGRAM_API_KEY and CARTESIA_API_KEY set on the Pi
#   - LiveKit docker images have finished pulling (or will during this run)
#   - hermes-gateway is already running on port 8642
#
# After running this, voice mode should work end-to-end. Run verify.sh
# afterwards to confirm.

set -euo pipefail

HERMES="${HERMES:-hermes}"
WORKSPACE="/home/vishnubv944/hermes-workspace"
HERMES_HOME="/home/vishnubv944/.hermes"

echo "==> 1/9: Checking SSH + target workspace..."
ssh "$HERMES" "test -d $WORKSPACE" || { echo "workspace not found"; exit 1; }
echo "    OK"

echo "==> 2/9: Creating ~/.hermes/{livekit,voice-agent}..."
ssh "$HERMES" "mkdir -p $HERMES_HOME/livekit $HERMES_HOME/voice-agent/logs $HERMES_HOME/voice-live-notes"
echo "    OK"

echo "==> 3/9: Copying LiveKit config..."
scp hermes-deploy/livekit/livekit.yaml "$HERMES:$HERMES_HOME/livekit/livekit.yaml"
scp hermes-deploy/livekit/docker-compose.yml "$HERMES:$HERMES_HOME/livekit/docker-compose.yml"
scp hermes-deploy/livekit/egress.yaml "$HERMES:$HERMES_HOME/livekit/egress.yaml" 2>/dev/null || true
echo "    OK"

echo "==> 4/9: Copying voice agent code..."
scp hermes-deploy/voice-agent/agent.py "$HERMES:$HERMES_HOME/voice-agent/agent.py"
scp hermes-deploy/voice-agent/requirements.txt "$HERMES:$HERMES_HOME/voice-agent/requirements.txt"
echo "    OK"

echo "==> 5/9: Verifying .env has voice-mode keys..."
ssh "$HERMES" "grep -E '^(DEEPGRAM_API_KEY|CARTESIA_API_KEY)=' $HERMES_HOME/.env" >/dev/null \
  || { echo ".env is missing DEEPGRAM_API_KEY or CARTESIA_API_KEY — add them first"; exit 1; }
echo "    OK"

echo "==> 6/9: Installing Python deps in venv..."
ssh "$HERMES" "test -x $HERMES_HOME/voice-agent/venv/bin/python" \
  || ssh "$HERMES" "cd $HERMES_HOME/voice-agent && python3 -m venv venv"
ssh "$HERMES" "cd $HERMES_HOME/voice-agent && ./venv/bin/pip install -q -r requirements.txt"
echo "    OK"

echo "==> 7/9: Pulling + starting LiveKit stack..."
ssh "$HERMES" "cd $HERMES_HOME/livekit && docker compose pull"
ssh "$HERMES" "cd $HERMES_HOME/livekit && docker compose up -d"
echo "    OK"

echo "==> 8/9: Installing workspace integration files..."
scp hermes-deploy/workspace-integration/voice-screen.tsx "$HERMES:$WORKSPACE/src/screens/voice/voice-screen.tsx"
scp hermes-deploy/workspace-integration/voice-route.tsx "$HERMES:$WORKSPACE/src/routes/voice.tsx"
ssh "$HERMES" "mkdir -p $WORKSPACE/src/routes/api/voice"
scp hermes-deploy/workspace-integration/voice-token-api.ts "$HERMES:$WORKSPACE/src/routes/api/voice/token.ts"
scp hermes-deploy/workspace-integration/voice-status-api.ts "$HERMES:$WORKSPACE/src/routes/api/voice/status.ts"
ssh "$HERMES" "mkdir -p $WORKSPACE/src/hooks"
scp hermes-deploy/workspace-integration/voice-hotkeys.ts "$HERMES:$WORKSPACE/src/hooks/use-voice-hotkeys.ts"
scp hermes-deploy/workspace-integration/continue-in-chat.tsx "$HERMES:$WORKSPACE/src/screens/voice/continue-in-chat.tsx"
ssh "$HERMES" "mkdir -p $WORKSPACE/src/routes/api/chat"
scp hermes-deploy/workspace-integration/chat-from-voice-api.ts "$HERMES:$WORKSPACE/src/routes/api/chat/from-voice.ts"
echo "    OK"

echo "==> 9/9: Installing + enabling systemd unit..."
scp hermes-deploy/voice-agent/hermes-voice-agent.service "$HERMES:$HERMES_HOME/voice-agent/hermes-voice-agent.service"
ssh "$HERMES" "mkdir -p ~/.config/systemd/user && \
  cp $HERMES_HOME/voice-agent/hermes-voice-agent.service ~/.config/systemd/user/ && \
  systemctl --user daemon-reload && \
  systemctl --user enable --now hermes-voice-agent.service"
echo "    OK"

echo
echo "============================================================"
echo "INSTALL COMPLETE."
echo "============================================================"
echo
echo "Next steps:"
echo "  1. Run verify.sh to confirm everything is up."
echo "  2. Open http://hermes:3001/voice — click the mic to test."
echo "  3. ssh hermes 'python3 ~/.hermes/voice-mode/cost-report.py'"
echo "     to see today's cost + latency."
echo

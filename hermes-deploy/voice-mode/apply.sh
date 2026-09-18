#!/usr/bin/env bash
# apply.sh — apply the hermes-workspace voice integration to the workspace repo.
#
# Run this from the repo root (D:\Projects\ultimate-brain-mcp on Windows,
# or whatever you cloned hermes-workspace to) AFTER:
#   1. LiveKit is up (docker compose up -d in ~/.hermes/livekit)
#   2. hermes-voice-agent is installed (pip install in ~/.hermes/voice-agent)
#   3. You have SSH access to hermes
#
# What it does:
#   - Adds the four voice files (screen, route, two API routes) to the workspace
#   - Patches chat-sidebar.tsx to add the Voice nav item
#   - Patches mobile-tab-bar.tsx to add the Voice tab
#   - Adds livekit-client to package.json
#   - Triggers a workspace rebuild
#
# Idempotent: re-running is safe (skips files that already match).

set -euo pipefail

HERMES="hermes"
WORKSPACE="/home/vishnubv944/hermes-workspace"
SRC="hermes-deploy/workspace-integration"

echo "==> Verifying SSH + target workspace..."
ssh "$HERMES" "test -d $WORKSPACE" || { echo "workspace not found at $WORKSPACE"; exit 1; }

echo "==> Copying voice screen..."
ssh "$HERMES" "mkdir -p $WORKSPACE/src/screens/voice"
scp "$SRC/voice-screen.tsx" "$HERMES:$WORKSPACE/src/screens/voice/voice-screen.tsx"

echo "==> Copying voice route..."
scp "$SRC/voice-route.tsx" "$HERMES:$WORKSPACE/src/routes/voice.tsx"

echo "==> Copying voice token API..."
ssh "$HERMES" "mkdir -p $WORKSPACE/src/routes/api/voice"
scp "$SRC/voice-token-api.ts" "$HERMES:$WORKSPACE/src/routes/api/voice/token.ts"

echo "==> Copying voice status API..."
scp "$SRC/voice-status-api.ts" "$HERMES:$WORKSPACE/src/routes/api/voice/status.ts"

echo "==> Patching chat-sidebar.tsx (adds Voice nav item)..."
ssh "$HERMES" "cat > /tmp/voice-sidebar-patch.py" <<'PYEOF'
import re, sys, pathlib
p = pathlib.Path("/home/vishnubv944/hermes-workspace/src/screens/chat/components/chat-sidebar.tsx")
src = p.read_text()
if "AiVoiceIcon" in src and "/voice" in src:
    print("already patched, skipping")
    sys.exit(0)
# Add AiVoiceIcon to the @hugeicons/core-free-icons import list
src = re.sub(
    r"(from '@hugeicons/core-free-icons')",
    r"\1 // voice-mode: AiVoiceIcon added below\nimport { AiVoiceIcon } from '@hugeicons/core-free-icons'\nconst _ensureVoiceIconUnused = AiVoiceIcon;",
    src,
    count=1,
)
# Add the nav item — find a workspaceItems.push({...}) block and insert after.
insertion = (
    "  { kind: 'link', to: '/voice', icon: AiVoiceIcon, label: 'Voice', active: pathname === '/voice' },\n"
)
if "to: '/voice'" in src:
    print("nav item already present")
else:
    src = src.replace(
        "dataTour: 'tasks'",
        "dataTour: 'tasks'" + insertion,
        1,
    ) if "dataTour: 'tasks'" in src else None
    if "to: '/voice'" not in src:
        # Fallback: append before the closing `]` of workspaceItems
        src = re.sub(r"(\n\]\s*$)", insertion + r"\1", src, count=1)
p.write_text(src)
print("patched")
PYEOF
ssh "$HERMES" "python3 /tmp/voice-sidebar-patch.py"

echo "==> Patching mobile-tab-bar.tsx (adds Voice tab)..."
ssh "$HERMES" "cat > /tmp/voice-tab-patch.py" <<'PYEOF'
import re, pathlib
p = pathlib.Path("/home/vishnubv944/hermes-workspace/src/components/mobile-tab-bar.tsx")
src = p.read_text()
if "AiVoiceIcon" in src and "/voice" in src:
    print("already patched, skipping"); raise SystemExit
src = re.sub(
    r"(from '@hugeicons/core-free-icons')",
    r"\1\nimport { AiVoiceIcon as _AiVoiceIcon } from '@hugeicons/core-free-icons'",
    src, count=1,
)
insertion = (
    "  { to: '/voice', icon: _AiVoiceIcon, label: 'Voice' },\n"
)
src = re.sub(r"(\]\s*export default)", insertion + r"\1", src, count=1)
p.write_text(src)
print("patched")
PYEOF
ssh "$HERMES" "python3 /tmp/voice-tab-patch.py"

echo "==> Adding livekit-client to package.json..."
ssh "$HERMES" "cd $WORKSPACE && pnpm add livekit-client@^2.5.0"

echo "==> Rebuilding workspace..."
ssh "$HERMES" "cd $WORKSPACE && pnpm build"

echo "==> Restarting workspace (if running under systemd)..."
ssh "$HERMES" "systemctl --user restart hermes-workspace.service 2>/dev/null || true"

echo
echo "DONE."
echo
echo "Next steps:"
echo "  1. Open http://hermes:3001/voice — you should see 'Voice unavailable'"
echo "     (livekit + gateway checks will fail until services are up)."
echo "  2. Start the voice agent: ssh hermes 'systemctl --user start hermes-voice-agent.service'"
echo "  3. Refresh /voice — click the mic button, allow permissions, talk."

---
name: deploy-hermes
description: >
  Make local edits to this ultimate-brain-mcp repo live for the Hermes agent
  running on this Pi. Use whenever the user asks to deploy, redeploy, "make
  this live", "push this to Hermes", restart Hermes so it picks up changes,
  or after any edit to src/ultimate_brain_mcp/*.py, pyproject.toml, or
  uv.lock in this checkout. Only relevant when running inside this repo on
  the Pi that hosts the live Hermes gateway (not the Windows dev machine).
---

# Deploy to Hermes

## Why this exists

This checkout **is** the code Hermes runs — `~/.local/bin/ub-mcp.sh` execs
`./.venv/bin/ultimate-brain-mcp` directly from this working tree, no
install/publish step in between. Editing a file here changes what Hermes
would run on its *next* process start.

The catch: the MCP server is a long-lived subprocess spawned once when
`hermes-gateway.service` starts, not re-spawned per message. So **editing
source alone does nothing until the service restarts** — that's the one
step this skill exists to make routine and safe.

## Steps

1. **Sync dependencies, only if changed.** If `pyproject.toml` or `uv.lock`
   changed, run:
   ```bash
   uv sync
   ```
   Skip this for pure `.py` edits — nothing to install.

2. **Smoke-test before going live** (recommended for anything touching
   startup/schema-discovery logic — `app_lifespan`, `_discover_*_schema`,
   config loading). Pull the real credentials straight from Hermes' own
   config rather than hardcoding them anywhere:
   ```bash
   python3 - <<'EOF'
   import yaml
   with open("/home/vishnubv944/.hermes/config.yaml") as f:
       cfg = yaml.safe_load(f)
   env = cfg["mcp_servers"]["ultimate-brain"]["env"]
   print("\n".join(f'export {k}="{v}"' for k, v in env.items()))
   EOF
   ```
   Eval that output, then:
   ```bash
   timeout 10 ./.venv/bin/ultimate-brain-mcp < /dev/null
   ```
   A clean run shows successful `HTTP Request ... 200 OK` schema-discovery
   calls and no traceback. Never echo the credential values themselves into
   chat or commit them anywhere — this repo is a public fork.

3. **Restart the gateway:**
   ```bash
   systemctl --user restart hermes-gateway.service
   ```

4. **Verify:**
   ```bash
   systemctl --user status hermes-gateway.service --no-pager | grep -E "Active|●"
   ps aux | grep ultimate-brain-mcp | grep -v grep
   ```
   Confirm the service is `active (running)` and the `ultimate-brain-mcp`
   process has a fresh PID/start time (proof the old one was actually
   replaced, not just still sitting there from before the edit).

## Not part of this flow

- **No `git pull` needed** — you're editing the deployed checkout directly.
- **Committing/pushing to the fork is a separate concern** (backup/history,
  not "going live"). Don't conflate the two — do it when it makes sense,
  but it's not required for changes to take effect on Hermes.
- If a change needs a brand-new env var (a new secondary database, etc.),
  that has to be added to `mcp_servers.ultimate-brain.env` in
  `~/.hermes/config.yaml` first — this skill doesn't cover schema/env
  changes, only redeploying existing code.

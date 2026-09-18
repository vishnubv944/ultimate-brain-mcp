# Autonomous Curator — Not a UB Concern

Context: [../hermes-agent-feature-surface.md](../hermes-agent-feature-surface.md),
[../pi-setup-audit.md](../pi-setup-audit.md).

The curator governs **agent-created skills** specifically — it consolidates overlapping
skills, marks unused ones stale after `stale_after_days` and archives them after
`archive_after_days` (recoverable, never auto-deleted), and leaves anything pinned alone.
It has no awareness of, and no effect on, Ultimate Brain data, task state, or anything in
`docs/hermes/`'s design.

Confirmed matching the Pi's actual config (`enabled: true`, `interval_hours: 168`,
`min_idle_hours: 2`, `stale_after_days: 30`, `archive_after_days: 90`) — nothing here
needs to change for UB integration. On a fresh install or after an update, the first
curator pass is deferred by a full `interval_hours`, giving a review window before it
touches anything — not relevant on this instance since it's already running.

The only place this could ever intersect UB work: if UB-specific behavior ever gets
authored as a Hermes **skill** (a reusable on-demand doc, distinct from AGENTS.md/SOUL.md
identity content) rather than baked into the identity/instruction layer, the curator
would eventually manage its lifecycle like any other agent-created skill. Nothing today
is designed that way, and there's no reason to force it — AGENTS.md is the right home for
standing UB instructions, not the skills system.

## Sources

- [Curator — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/curator)

## Status

Confirmed no action needed. Nothing here changes as part of UB integration.

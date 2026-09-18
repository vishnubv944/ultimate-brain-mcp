# Subagent Delegation — Where It Actually Fits the UB Design

Context: [../hermes-agent-feature-surface.md](../hermes-agent-feature-surface.md),
`docs/hermes/eod-review.md`, `mode-trigger-map.md`. None of the current cron jobs or
designed modes use delegation today. This is an assessment of whether any UB ritual
should, not a requirement to adopt it.

## How it actually works (specifics beyond the feature-surface summary)

- **No conversation history passes to a subagent.** It sees only the goal and context the
  parent explicitly hands it — a subagent used for UB work would need every relevant ID
  (task IDs, the snapshot data, the date range) passed in explicitly, not assumed
  available.
- **Subagents inherit the parent's enabled toolsets** — `delegate_task` has no
  model-facing parameter to grant extra capabilities. Since `inherit_mcp_toolsets: true`
  is already set on the Pi, a delegated child automatically gets the same `ultimate-brain`
  MCP tools the parent has — no extra wiring needed if delegation is ever used for UB
  work.
- **`max_spawn_depth: 1`, `max_concurrent_children: 3`, `orchestrator_enabled: true`,
  `subagent_auto_approve: false`** — confirmed as the current Pi config. Depth 1 means
  nested delegation (a subagent delegating further) is off by default; each extra depth
  level multiplies potential concurrent spend, so raising it should be deliberate, not
  default.
- Intended use: subtasks that need **reasoning, judgment, or multi-step problem solving**
  — not simple single tool calls.

## Where this could genuinely fit

**A deep weekly pattern-analysis pass** — reviewing the `Hermes Log` for repeated
off-checkpoint replan attempts, correlating them by trigger, across a longer window than
a single week — is the one candidate that fits the stated use case cleanly: it's
multi-step, benefits from working in isolation without cluttering the live weekly-review
Telegram thread with intermediate tool calls, and only the conclusion ("this trigger has
shown up 4 times this month") needs to surface back to you. This maps directly onto
`mode-trigger-map.md`'s Review + Event-triggered cell ("3+ logged replan attempts with
the same trigger").

**Where it doesn't fit**: any of the interactive daily rituals (Plan, Wrap-Up, live
Planning discussion). Those are explicitly conversational and low-latency by design
(`daily-plan-phase.md`'s "~90 second" pattern, planning-mode.md's back-and-forth) — adding
a subagent hop would work against that, not for it. Delegation is for offloading a
self-contained analytical task, not for running the core daily loop.

## Verdict

Not needed for anything currently designed. Worth introducing specifically for the
weekly pattern-analysis step once the Hermes Log actually has enough data in it to
analyze — premature before then.

## Sources

- [Subagent Delegation — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/delegation)
- [Delegation & Parallel Work — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/guides/delegation-patterns)
- [Subagent Delegation — DeepWiki](https://deepwiki.com/NousResearch/hermes-agent/5.7-subagent-delegation)

## Status

Assessed, not adopted. Revisit once the Hermes Log has real data to analyze.

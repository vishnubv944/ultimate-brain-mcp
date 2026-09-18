# /personality — Why It Should Stay Out of the Way

Context: [soul-md.md](soul-md.md), [../hermes-agent-feature-surface.md](../hermes-agent-feature-surface.md).
Concrete recommendation, not a feature description.

## How it actually works

`/personality` is a **session-level overlay** — it changes or supplements the current
system prompt for that session only, without touching the durable identity in SOUL.md.
It switches between the presets defined in `config.yaml`'s `personalities:` dict.
Nous Research's own guidance: "keep a thoughtful global SOUL.md... use `/personality`
only when you want a temporary mode shift." SOUL.md is the baseline; `/personality` is
for occasionally stepping outside it, not for defining it.

## What's currently configured (per pi-setup-audit.md)

The `personalities:` dict on the Pi is entirely generic template boilerplate —
`helpful`, `concise`, `technical`, `creative`, `teacher`, `kawaii`, `catgirl`, `pirate`,
`shakespeare`, `surfer`, `noir`, `uwu`, `philosopher`. None of these have anything to do
with Ultimate Brain or the life-management identity — they're leftover defaults from
whatever base install Hermes ships as.

## Recommendation: don't build a UB-flavored personality preset

The instinct might be to add a `ub-mode` or `life-manager` entry to this dict as "the
real personality." That would be a mistake, for a structural reason: `/personality` is a
**temporary, session-scoped override** — it exists specifically for moments where you
want to step *outside* the default identity. If the UB-native identity is only reachable
by explicitly invoking a personality override, that's the opposite of "fused" — it
signals UB-awareness is optional, one command away from being switched off to something
else. **The fusion has to live in SOUL.md, which is always on, not in a personality you
have to remember to select.**

The generic joke presets aren't actively harmful sitting unused in config — they're just
dead weight. Two reasonable options, no strong pull toward either:
- Leave them as-is; they cost nothing unless invoked
- Prune them down to reduce clutter in `config.yaml` and remove any chance of
  accidentally triggering "pirate mode" during a real planning session

## The one legitimate use of `/personality` here

If there's ever a genuine need for Hermes to temporarily *not* be in full life-management
mode — e.g., a pure technical/coding question that has nothing to do with UB — a light
`technical` or `concise` override for that single session is a reasonable use of the
mechanism as designed. That's different from needing a UB-mode toggle, because the
default (SOUL.md) is already UB-native; `/personality` would just be stepping briefly
away from it, not into it.

## Sources

- [Personality & SOUL.md — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/personality)
- [Which File Does What — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/which-file-does-what)

## Status

Recommendation: no changes needed to `/personality` or the `personalities:` dict beyond
optional cleanup. The real work is entirely in [soul-md.md](soul-md.md).

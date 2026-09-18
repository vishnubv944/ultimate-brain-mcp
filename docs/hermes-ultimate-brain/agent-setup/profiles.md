# Profiles — Relevance to Ultimate Brain Fusion

## What it is

A Hermes **profile** is a fully separate agent home directory — its own `config.yaml`,
`.env`, `SOUL.md`, memories, sessions, skills, cron jobs, and state database. Each
profile needing to run continuously (e.g. listening on Telegram) needs its own gateway
service/process; Hermes's own guidance is explicit that two agent processes should never
point at the same profile. Created with `hermes profile create`, targeted per-command
with `hermes -p <name>`, or set as the sticky default with `hermes profile use <name>`.

## The actual question: should personal life-management get its own profile?

Arguments for a dedicated profile: total isolation of SOUL.md, memory, and sessions from
anything else Hermes might ever be asked to do — no risk of an unrelated task polluting
the identity or memory that's being built specifically around Ultimate Brain and
PURPOSE.md.

Arguments against, and the deciding factors:

- **There is no competing use case to isolate from.** The default profile on the Pi
  today isn't serving some other purpose that UB-fusion would contaminate — it's a
  single-user Raspberry Pi setup, and the entire SOUL.md/cron/MCP work under way is
  explicitly *for* this life-management purpose. Isolation only has value when there's
  something real to isolate from.
- **Running a second profile means a second gateway process**, each with its own
  systemd service, memory footprint, and model/provider connections — real resource
  overhead on a Pi, for a benefit that doesn't currently exist.
- **A separate profile would actually work against the stated goal.** The whole point of
  this effort is that Ultimate Brain *is* who Hermes is on this instance, not one
  identity among several it happens to run. Splitting it into its own profile frames it
  as one workload alongside others, the opposite of fusion.

One thing worth noting from the Pi audit: `mcp_servers` in `config.yaml` includes a
`vault-mcp` entry (currently `enabled: false`). If that or anything else ever becomes a
genuinely separate concern from personal life-management — a different identity, a
different tool surface — *that* would be the actual trigger for a profile split, not UB
itself.

## Recommendation

Stay on the single default profile. Put SOUL.md, AGENTS.md, and all the UB-fusion work
directly into it — that's what makes it *the* identity, not *a* identity. Revisit only if
a genuinely separate, unrelated use case for this Hermes instance shows up later (e.g. it
becomes a general dev-assistant on top of also being a life-management agent, and the two
identities start to conflict).

## Sources

- [Profiles: Running Multiple Agents — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/profiles)
- [Running Many Gateways at Once — Hermes Agent docs](https://hermes-agent.nousresearch.com/docs/user-guide/multi-profile-gateways)

## Status

Assessed and decided: no profile split. Default profile is where all UB-fusion work
(SOUL.md, AGENTS.md, cron reconciliation) should live.

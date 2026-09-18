# Toolsets — Removing What Competes With Ultimate Brain

Context: [../hermes-agent-feature-surface.md](../hermes-agent-feature-surface.md),
[../pi-setup-audit.md](../pi-setup-audit.md), [../../hermes/PURPOSE.md](../../hermes/PURPOSE.md).
Ultimate Brain is meant to be the single system of record. Any built-in Hermes toolset
that duplicates what UB already does is a second place the model can reach for instead —
that's the opposite of "lives and breathes Ultimate Brain."

## The fix: `agent.disabled_toolsets`

Confirmed in Hermes's own docs and config reference. Must be nested under `agent:` —
a bare root-level `disabled_toolsets:` fails silently (a known, filed Hermes bug,
[#97111](https://github.com/NousResearch/hermes-agent/issues/97111)), as does any
misspelled/unknown toolset name. This suppresses the listed toolsets across the CLI and
every gateway platform (Telegram, Discord, etc.) in one place, and it applies *after*
whatever `platform_toolsets` says — so it's a hard override, not something that can be
silently re-enabled by a stale per-platform list.

```yaml
agent:
  disabled_toolsets:
    - todo
```

## Full audit of the toolset list, not just `todo`

Checked every toolset in the feature-surface doc's list against what Ultimate Brain
already covers:

| Toolset | Overlaps UB? | Verdict |
|---|---|---|
| `todo` | **Yes — direct.** A second native task list, separate from Tasks DB. | Disable. This is the one already flagged in the audit and it's the clearest case. |
| `memory` | Partial — Hermes's own cross-session recall (`MEMORY.md`/`USER.md`) vs. UB's structured PARA data (Notes/Goals/Tags). Different *kind* of data (conversational facts about you vs. structured life-management records), not a duplicate system. | Keep, but see the open boundary question below — don't disable blind. |
| `session_search` | No — searches past conversation history, not life data. | Keep. |
| `homeassistant` | No — smart-home control, unrelated domain. | Keep (or disable per-platform if unused, not a UB-competition issue). |
| `spotify` | No — music control. | Keep. |
| `cronjob` | No — this *runs* the Hermes-side rituals (morning/nightly/weekly) that write into UB; it's an enabler, not a competitor. | Keep. |
| `delegation`, `code_execution`, `clarify`, `web`, `search`, `terminal`, `file`, `browser`, `vision`, `image_gen`, `tts`, `debugging`, `safe`, `messaging`, `discord`, `discord_admin` | No — none of these manage tasks/projects/notes/goals. | Keep. |

**Verdict: `todo` is the only toolset that's a genuine duplicate system and should be
disabled.** Everything else either serves a different domain entirely or actively
supports the UB-native rituals rather than competing with them.

## The `memory` boundary — flagged, not resolved

Disabling `memory` outright would be a mistake — it's not a competing task/project
system, it's Hermes's own recall of who you are across sessions, which is a legitimate
and different function from UB's structured data. But *where the line sits* between "a
preference Hermes should just remember" and "a fact that belongs in a UB Note/Goal" isn't
defined anywhere yet. Leave `memory` enabled; the boundary itself is SOUL.md/AGENTS.md
content to write, not a toolset to flip off.

## What this doesn't fix

Disabling `todo` removes the *native* competing tool. It does nothing about the
`todoist-inbox-sync` cron job, which is a second external capture system feeding into UB
rather than a Hermes-internal one — that's a separate decision already flagged in
pi-setup-audit.md, not something `disabled_toolsets` touches.

## Status

Config change identified, not yet applied on the Pi. `todo` → `agent.disabled_toolsets`
is a one-line, low-risk change. Everything else in the table is a deliberate keep, not an
oversight.

Sources:
- [Toolsets Reference — Hermes Agent](https://hermes-agent.nousresearch.com/docs/reference/toolsets-reference)
- [Hermes Agent Configuration](https://hermes-agent.nousresearch.com/docs/user-guide/configuration/)
- [Issue #97111 — disabled_toolsets fails silently at wrong nesting](https://github.com/NousResearch/hermes-agent/issues/97111)
- [Nous Research: Blank Slate Mode via platform_toolsets/disabled_toolsets — MarkTechPost](https://www.marktechpost.com/2026/06/20/nous-research-updates-hermes-agent-with-a-blank-slate-mode-that-pins-toolsets-via-platform_toolsets-cli-and-disabled_toolsets/)

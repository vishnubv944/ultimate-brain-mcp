# Research — how people actually run task/project/goal management with an AI agent

Status: reference material, gathered via web research to ground the
Hermes/UB direction in what's known to work and fail elsewhere, rather
than guessing. Not a spec — see `GAPS.md` for the implementation gaps this
surfaced and `README.md` for the overall plan.

Two source types: (1) the actual open-source **Hermes Agent** (Nous
Research) — a self-hosted, MCP-native personal agent, very likely the
literal thing this MCP is meant to connect to — and its own documented
user patterns; (2) general research on productivity systems (GTD, weekly/
monthly/quarterly reviews, OKRs) and known AI-agent-on-Notion failure
modes.

---

## 1. Hermes Agent (Nous Research) — what its own users report

Hermes Agent is a self-hosted, open-source autonomous agent with
persistent memory, a skills system, built-in cron/scheduling, and native
MCP support — connect any MCP server (this one included) without
modifying its source.

### What works
- **Sub-agent per project, not one flat list.** Users run a "Chief of
  Staff" main agent plus per-project sub-profiles, each with separate
  memory: *"Every 'project' (1 project = 1 Slack channel) has its own
  agent sub-profile with its own memory."*
- **Skills over memory for repeatable workflows.** After a complex
  multi-step task (5+ tool calls, a tricky fix, a non-trivial workflow),
  users save the successful approach as a reusable skill rather than
  trusting the agent to "remember" it in conversation next time.
- **Kanban-based handoff** between a parent agent and child sub-agents —
  parent posts cards, children pull and report back — works well for
  parallelizing work across projects.
- **Natural-language scheduling** ("every weekday at 9am, summarize my
  inbox") — the agent owns *when* a recurring workflow runs, not the user
  manually configuring cron.

### What doesn't work
- **Context compression silently drops constraints.** Directly quoted
  from a user: *"After ~30 turns, context compression silently removes
  older messages. A constraint decided at turn 5 is gone by turn 50."*
  This is the single most concrete, actionable finding from Hermes' own
  users. It fails silently — the agent keeps running, nothing errors, but
  a rule or fact from earlier in the session is just gone.
- **No proactive nudging.** The agent is reactive, not proactive — it
  doesn't re-engage the user on its own; users explicitly ask for "a bit
  more alive, able to gently re-engage" behavior.
- **Approval-gate friction** in multi-user/compliance setups — human-in-
  the-loop gates add real friction to otherwise-autonomous operation.
- **Hermes Agent's own docs have zero task/project/goal-management
  guidance** — no review cadence, no goal framework, nothing. It's a
  general-purpose agent runtime; it brings memory, skills, and scheduling,
  not a life-management data model. That gap is exactly what Ultimate
  Brain (via this MCP) is meant to fill.

**Implication:** since the agent's own working memory is not reliable
across a long session (context compression), anything Hermes needs to
keep honoring — today's plan, an in-progress constraint, GTD state —
needs to live in Notion via this MCP's tools, readable back out cheaply,
rather than assumed to persist in conversation. This directly motivates
surfacing more state through typed tools (see Gap B2 in `GAPS.md`: Smart
List, Snooze/Wait Date, Energy) instead of leaving it to generic
fallback calls the agent has to remember to make.

---

## 2. GTD / weekly-review research

- **The weekly review is the highest-leverage habit, and the #1 failure
  mode is silently skipping it.** A Dominican University study found
  people who wrote down goals and reviewed them weekly were 76% more
  likely to achieve them. Conversely, the most common reason GTD systems
  fail: the weekly review gets skipped, the system quietly goes stale,
  the user stops trusting it, and abandons it without ever making a
  conscious decision to.
- **Vague capture erodes trust.** "Plan the offsite" is not a next
  action; "book a venue" is. Systems that let vague, blob-like tasks
  accumulate become intimidating and get avoided.
- **Overcomplication (e.g. too many GTD contexts) is a known aging-badly
  pattern** — simpler beats exhaustive.

**Implication for an agent doing this on the user's behalf:** an AI
agent doesn't get tired and skip the weekly review the way a human does
— that's a structural advantage — but only if something is actually
calling a weekly-rollup-equivalent tool on a real cadence, not just
making one available. Also reinforces: task creation should push toward
actionable phrasing, not just store whatever text the user or agent
generated verbatim.

---

## 3. Quarterly OKRs / goal-setting research

- **Individual/personal OKRs have failed at scale even at OKR-native
  companies.** Both Google and Spotify tried and dropped personal OKRs.
  Documented reasons: quarterly cycles are too slow for real feedback
  (teams discover assumptions were wrong only late in the cycle), and
  mixing "output" goals with personal-growth goals in one list creates
  confused prioritization.
- **What's reported to work instead: frequent check-ins on the same
  goals** (weekly or at minimum monthly), not periodic goal-setting in
  isolation followed by silence until the next cycle.

**Implication:** Ultimate Brain's actual Goals model (Dream → Active →
Achieved, with a Milestones relation) is already closer to
continuous-check-in than to quarterly-OKR-reset, which is the right
shape per this research. The risk is on our side, not UB's schema: if
Goals/Milestones progress isn't part of whatever rollup Hermes calls
regularly, they'll silently go stale exactly the way research says
quarterly goals do. Nothing here suggests building OKR-style quarterly
machinery on top — it suggests making sure Goals/Milestones surface in a
*regular* (weekly-ish) rollup call, not a separate quarterly ritual.

---

## 4. AI agent + Notion-style task management — known failure modes

- **Duplicate task creation** — no stable notion of "is this the same
  task as one that already exists" across repeated runs or partial
  context.
- **Tool-call hallucination** — wrong tool selection, malformed
  arguments, or assuming an action succeeded when it didn't (functional
  hallucination, distinct from factual hallucination).
- **Task drift** — the agent completes the literal instruction while
  missing the actual intent behind it.
- **Context drift attributed as a leading cause of enterprise AI agent
  failure** — reported as behind roughly 65% of multi-step-reasoning
  failures, ahead of raw context-window exhaustion.
- **Mitigations reported to actually help:** draft-and-confirm for
  ambiguous actions (create as a draft, let the user/agent confirm before
  it's final) vs. direct execution only for well-defined, low-ambiguity
  actions; strict schema/parameter validation on tool calls; logging/
  observability so failures are debuggable after the fact rather than
  silent.

**Implication:** this MCP's typed tools (which validate/coerce specific
parameters) are already safer than the generic `update_page` fallback in
this respect. Worth treating "does something like this already exist"
checks and stricter validation as a real design concern once Hermes is
creating tasks/notes autonomously and unattended — not just as an
afterthought.

---

## New candidate gaps surfaced by this research (added to `GAPS.md` as Gap C)

1. No duplicate-detection on `create_task`/`create_note` — directly
   named as a common AI-agent-on-Notion failure mode.
2. No cheap, narrowly-scoped "everything currently relevant to project
   X" call distinct from the heavier `get_project_detail` — relevant to
   the sub-agent-per-project pattern Hermes users report actually using.
3. No canonical *weekly* rollup that includes Goals/Milestones progress
   — `daily_review_snapshot` exists but its name and current scope are
   daily-task-focused; the review-cadence research above says Goals/
   Milestones need to appear in a *regular*, not quarterly-only, review
   too.

---

## Sources

- [Hermes Agent Documentation](https://hermes-agent.nousresearch.com/docs/)
- [User Stories & Use Cases | Hermes Agent](https://hermes-agent.nousresearch.com/docs/user-stories)
- [GitHub - NousResearch/hermes-agent](https://github.com/nousresearch/hermes-agent)
- [10 Reasons Why GTD Might Be Failing](https://facilethings.com/blog/en/why-gtd-fails)
- [Quarterly OKRs are broken: What to do instead](https://xodiac.ca/blog/articles/quarterly-okrs-are-broken-what-to-do-instead)
- [Why individual OKRs don't work for us (Spotify)](https://hrblog.spotify.com/2016/08/15/our-beliefs)
- [How Hermes and Claude Handle Context Compression in Agents](https://mem0.ai/blog/how-hermes-and-claude-handle-context-compression-in-real-production-agents-(and-what-you-should-extract))
- [AI Agent Failure Modes That Are Killing Productivity](https://www.squaredtech.co/ai-agent-failure-modes-the-surprising-mess-beyond-hallucination)

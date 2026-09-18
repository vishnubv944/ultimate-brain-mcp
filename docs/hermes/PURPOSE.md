# Why Hermes Exists

## The problem

I've been trying to learn AI Fundamentals for 2+ years and have changed my study routine
50+ times in that span. The pattern isn't unique to this goal — it shows up any time I'm
running something end-to-end on my own:

- I'm a strong planner: given a situation, I can figure out what needs to happen to get
  the outcome.
- I'm a strong executor: if someone else hands me a plan, I run it like a charm.
- I cannot do both at once, myself, on my own initiative. I either stay in planning mode
  indefinitely, or I start executing and pull myself back into "let me just optimize this
  one thing" — which is planning again, wearing execution's clothes.

## Root cause (self-diagnosed, not distrust)

It's perfectionism, not distrust of my own plans and not fear of a wrong plan. I'm
confident in what I plan. The block is: I'm desperate to execute, but I won't let myself
start until the plan is *perfect* — and "perfect" has no stopping condition, so I never
arrive at permission to start.

This matches a documented pattern: the **Perfectionism → Procrastination → Paralysis
cycle**. Planning feels productive and carries no risk of a visibly wrong outcome, so the
bar for "done planning" keeps moving. It isn't a rational planning gap — the plan is
usually already good enough — it's that I retain permanent edit access to my own plans,
so there's always one more revision available, and I keep taking it instead of shipping.

The tell: I execute flawlessly on an externally-fixed plan (someone else's) and
indefinitely on my own, because my own plan is never actually fixed.

## What actually breaks this cycle (research-backed)

- **The 70% rule** (Bezos, Marine Corps doctrine, PM practice): execute once you have
  ~70% of the clarity you'd want, not 95%+. A 70%-clear plan executed today beats a
  95%-clear plan executed never — the last 30% of certainty is mostly only obtainable
  *through* execution, not more planning.
- **Time-boxed planning, not quality-boxed planning**: plan for a fixed window, then run
  with whatever exists at the buzzer. "Plan until it feels perfect" has no exit condition
  for me; "plan until the timer ends" does.
- **Commitment devices**: remove my own write-access to the plan mid-execution.
  Well-established outside this context too (Pomodoro timers, public commitments,
  Ulysses contracts) — the fix for perfectionism-driven paralysis is structural, not
  willpower.
- **Relational accountability**: ADHD/task-management research found static checklists
  and reminders don't reproduce the pressure of another entity actually watching —
  conversational check-ins and real-time acknowledgment work where a checkbox doesn't.
  This is why an agent *conversation* has a real shot where a Notion checkbox hasn't.
- **Scoped replanning** (from agentic-AI systems research, independently converging on
  the same shape): a local failure should trigger a local fix, not a full re-plan. If one
  task is blocked, fix that task — don't reopen the whole week.

## What Hermes is for

Hermes is the external structure that holds my plan still once I've set it, so my
perfectionism has nothing left to grab onto mid-execution. Concretely:

- Hermes and I plan together and commit the result to Ultimate Brain. Once committed,
  the plan is locked until the next checkpoint.
- During execution, Hermes's job is check-ins and unblocking — not renegotiating the
  plan. A request to "just tweak this one thing" outside a checkpoint should be named as
  a re-plan attempt and redirected, not quietly honored.
- Checkpoints are real and scheduled (not "never"), so the planning instinct has a
  legitimate, bounded outlet instead of being suppressed entirely. Likely shape: daily
  check-in (progress + blockers, no plan edits) + weekly review (plan edits welcome).
- Off-checkpoint replan attempts get logged, not just blocked — over time that log
  reveals whether a given urge was a genuinely bad plan or reflexive perfectionism.

## Status

This is a working thesis under active brainstorming, not a finished design. Open
questions to resolve next: what "locked plan" looks like as a concrete UB task/project
structure, what Hermes's exact conversational rules are for detecting and deflecting an
off-checkpoint replan request, and how the deviation log gets surfaced back to me.

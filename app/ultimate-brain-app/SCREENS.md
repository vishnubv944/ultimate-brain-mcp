# Screens

Every screen in the app (`AppScreen` enum in `MyDayViewModel.kt`), one line each — plus, per screen, what the best app in that specific use-case category does for UI/UX and what we should borrow or already match. Sources are listed at the bottom.

## Bottom-nav tabs

- **Today** — the daily home surface: today's shortlist, "add to today" browse chips (Inbox/Today/Active projects/Week/Month/Overdue/…), and the evening wrap-up.
  - **Research (Things 3):** its Today view is deliberately *not* auto-populated from due dates — nothing shows up unless the user put it there on purpose, which is exactly our My Day model already. Things also keeps each Today task's parent project visible inline so the list stays scannable without opening anything. **Borrow:** make sure every My Day row always shows its project name (already does), and resist ever auto-adding overdue/due-today items into the count without the user choosing it.

- **Tasks** — full task list with the same filter-chip row as Today, grouped and sortable.
  - **Research (Todoist):** priority is a visual signal, not a sort key on its own — in Todoist's Today/Upcoming views, higher-priority tasks float toward the top *within* the date grouping rather than creating a separate priority-sorted list. **Borrow:** worth checking whether our "Today" filter should nudge High priority tasks upward within the list instead of leaving pure due-date order.

- **Projects** — all projects grouped by status (In progress / To-do / Complete) with status filter chips.
  - **Research (Linear):** empty states stay minimal — "no open issues" gets a short line and a single "Create issue" button, no illustration, no tutorial, because it trusts the user already knows what a project is. **Borrow:** our empty-project-list state should stay this plain; avoid adding decorative empty-state art later.

- **Chat** — the Hermes AI assistant: streaming replies, tool-call chips, a "/" command palette (built-ins + live skills), image/document attach.
  - **Research (Raycast/Spotlight command palettes):** keyboard/search-first, tight consistent row spacing, uniform row heights, restrained corner radii ("modern but tool-like"), and — critically — acting on a result immediately rather than adding a confirmation step. Already the shape of our "/" palette; the remaining gap is making tool-chip rows as tight/uniform as a real command-palette row.

## Tasks

- **Task detail** — one task's properties (status, project, due, priority, My Day), description, sub-tasks, recurrence history, and time-tracking tabs.
  - **Research (Todoist):** the task view is deliberately "everything in one place" — description, date, priority, labels, sub-tasks, attachments, reminders — with zero context-switching to get any of it, and sub-tasks have their own show/hide-completed toggle so a long checklist doesn't overwhelm the parent task. **Borrow:** consider a "hide completed sub-tasks" toggle on our Sub-Tasks tab once a task accumulates many of them.

## Projects

- **Project detail** — one project's properties (status, deadline, progress, goal), linked open/done tasks, and notes.
  - **Research (Linear):** lean visuals, immediate state changes, no modal round-trips for common edits — a status change should apply and reflect instantly, not open a confirmation sheet. This matches our new row-based property list (tap → inline dropdown, no dialog for selects).
- **Edit project** — form for a project's core fields (status, deadline, tags, review notes).
  - **Research (settings-form UX guidelines):** put the most-used field first, group related fields with a header/divider rather than one long flat list. Our status/deadline/tags/review-notes order already follows "most used first."

## Notes

- **Notes** — note list with filter chips (Inbox, Favorites, Clips, Voice, Journal, Meetings).
  - **Research (Notion vs. Bear vs. Obsidian):** Notion wins for "one workspace with everything," but Bear is called out specifically for typography and a clean writing environment, and Obsidian for a distraction-free vault view. Our filter-chip row is closer to Notion's database-view model, which fits since notes here are one type among many entities, not a standalone app.
- **Note detail** — one note's properties (type, date, project, favorite), rendered markdown body, linked tasks.
  - **Research (Bear):** clean typography and generous whitespace are what make its reading/writing experience feel "beautiful" more than any feature — worth revisiting our `MarkdownBody` type scale/line-height for note bodies specifically (as opposed to the denser property rows above it).
- **Note editor** — full-screen markdown editor for writing/editing a note's body.
  - **Research (Bear/Obsidian):** both keep editor chrome minimal — no visible toolbar by default, formatting via Markdown syntax or a hidden bar — so the page reads as "just the text." Matches our borderless editor approach.

## Goals

- **Goals** — goal list with sort/filter chips (by activity, deadline, tag) plus Achieved/Dropped.
  - **Research (Streaks — Apple Design Award winner):** its entire design stays minimal because it protects one single number (the streak) as the motivator — it doesn't try to show every metric at once. **Research (Spark):** puts one key metric (goal achievement rate) on the home screen instead of a dashboard of stats.
  - **Borrow:** our goal list's aggregated-progress percentage is the right single number to lead with per row; resist adding more per-row stats that would dilute it.
- **Goal detail** — one goal's properties (status, area, progress, deadline), linked projects and milestone stats.
  - **Research (Spark):** simplify to the one number that matters (progress) shown big, everything else secondary. Our `FieldRow` for Progress already does this; keep it as the most prominent row, not just alphabetically wherever it falls.
- **Milestones** — every milestone across all goals, filterable by status (in progress / completed / pending).
  - **Research (Asana):** renders milestones as a visually distinct shape (a diamond) specifically so they don't get confused with ordinary tasks in a list. **Borrow:** worth giving our milestone rows a distinct marker (not just a status dot) so they read as "a milestone" at a glance among tasks/projects elsewhere in the app.
- **Milestone detail** — one milestone's status, target date, and linked tasks.
  - **Research (Asana):** best practice is to keep the actual checklist work on regular tasks and use the milestone purely as the marker/target — plus every milestone should have a clear target date and an owner. Matches our model (milestone links tasks, doesn't duplicate their content).

## Tags

- **Tags** — tags/areas grouped by type (Area / Resource / Entity), with favorites and A–Z sort.
  - **Research (Notion's own PARA method):** Areas are explicitly "ongoing responsibilities with no deadline," distinct from Projects (time-bound, a finish line) — and PARA workspaces lean on properties/tags for filtering rather than deep folder nesting. Our Area/Resource/Entity split already mirrors this; the type filter chips are the PARA-correct way to browse it.
- **Tag detail** — one tag's type/favorite properties, its sub-tags, and everything tagged with it (projects, notes, goals).
  - **Research (PARA):** a tag/area page's job is to be a hub back to everything active under it — which is exactly our "Projects / Notes / Goals tagged here" sections.

## Work sessions

- **Work sessions** — history of every logged focus session across all tasks.
  - **Research (Toggl Track):** its List view keeps entries groupable/ungroupable by project with bulk-editable fields (description, project, tags, duration) in one flat, filterable list rather than a calendar. Our flat session list matches this; grouping by project/day as an option would be the next Toggl-style addition.
- **Task work sessions** — logged focus-session history for one specific task.
  - **Research (Toggl Track):** scoping the same list view down to one project/task, rather than a different UI, is exactly Toggl's own drill-down pattern — which is what we already do (same row shape, filtered).

## Library (books, people, food)

- **People** — list of people/contacts in the workspace.
  - **Research (Clay / Dex — personal CRM apps):** the best ones make "add a contact and log an interaction" a few-second action, and surface *recent* interaction context rather than a static contact-card. Our People list is currently closer to a plain contact list; a "last noted" line per row (like Dex) would be the natural next step, not required for parity today.
- **Person detail** — one person's page.
  - **Research (Dex/Clay):** show recent notes/interactions front and center, not buried under a generic profile layout.
- **Books** — reading-library book list.
  - **Research (StoryGraph):** built around stats, mood, and pace rather than a static shelf — but reviewers specifically flag its navigation as "sharp and unwelcoming," with a comprehensive book list buried behind non-obvious paths. **Borrow the stats-forward idea, avoid its specific mistake:** keep our book list one flat, obvious list — don't split it across mood/status tabs the way StoryGraph's home screen does.
- **Book detail** — one book's page.
  - Same StoryGraph caution applies at the detail level: keep primary actions (update progress, rate) one tap away, not nested.
- **Reading log** — dated log entries of reading progress (pages read, per book).
  - **Research (StoryGraph):** its differentiator is turning log entries into charts/graphs of reading pace over time. Out of scope for now, but the dated, per-book log entry list we have is the right raw data shape to eventually chart.
- **Genres** — book genre list with per-genre book counts.
  - **Research (StoryGraph):** genre/mood is a *discovery* filter more than a static list — our per-genre book count is the minimal useful version of that.
- **Recipes** — recipe list.
  - **Research (Paprika):** "the gold standard for a digital recipe collection" — fast library, scaling, clipping. Our flat searchable list matches Paprika's core list view; scaling/clipping are beyond this app's scope.
- **Recipe detail** — one recipe's page.
  - **Research (Paprika):** ingredients and steps need to stay legible while scaled/scrolled mid-cook — clear separation between ingredients and steps, big touch targets. Worth checking our recipe detail keeps that separation visually distinct, not run together as prose.
- **Meal planner** — dated meal plan linking recipes to breakfast/lunch/dinner slots.
  - **Research (Mealime):** wins on "install and start cooking" — a guided weekly calendar that auto-populates a grocery list from the plan. Our meal planner is a plan-only view; grocery-list generation would be the Mealime-style next step, not required now.

## System

- **Routines** — scheduled Hermes prompts (cron jobs): list, create, pause/resume, run now.
  - **Research (Apple Shortcuts / IFTTT):** automations need consistent, descriptive naming to stay manageable as the list grows, and should be manually runnable on demand in addition to their trigger. Our Routines screen already has consistent name+schedule rows and a "Run now" action — matches the reference pattern directly.
- **Global search** — one search box across tasks, projects, notes, tags, and goals.
  - **Research (Raycast/Spotlight):** a single-column list, search-first, minimal chrome, acting on a result the instant it's tapped rather than a two-step "select then confirm." Matches our current implementation; the main gap vs. Raycast is visual tightness (uniform row height, compact padding) worth another pass on later.
- **Settings** — Notion/Hermes connection status, theme toggle, sync controls, debug info.
  - **Research (Android/iOS settings guidelines):** group related settings under a header with a divider rather than one flat list, and put the most-used items first; add a search box only once the list gets long enough to need scrolling more than once. Our Notion/Hermes/theme grouping already follows the "group by header" rule; no changes needed at current list length.

## Time-block (also under Tasks)

- **Time-block** — drag-and-drop hourly schedule for today's My Day tasks (or another day's due tasks).
  - **Research (Structured vs. Sunsama):** Structured is the reference for exactly this screen shape — a visual hourly timeline where every task maps to a block so the whole day is visible at a glance, for people who "think in timelines." Sunsama's alternative (a calmer, manual kanban ritual) is a different product philosophy, not a better version of this same screen. **Verdict:** our screen is already Structured's pattern; the fix already shipped (all of My Day appears here now, not just due-dated tasks) was the correct direction — Structured never gates a timeline slot behind a due date either.

## Sources

- Things 3: [App Store](https://apps.apple.com/us/app/things-3/id904237743), [culturedcode.com](https://culturedcode.com/things/features/)
- Todoist: [task view](https://todoist.com/help/articles/use-the-task-view-to-manage-tasks-in-todoist-eDeRDO0C), [sub-tasks](https://www.todoist.com/help/todoist/features/use-sub-tasks-in-todoist-kMamDo), [priority](https://www.todoist.com/help/articles/set-a-priority-in-todoist-Wy82Jp)
- Linear: [saasui.design](https://www.saasui.design/application/linear), [eleken.co mobile UX examples](https://www.eleken.co/blog-posts/mobile-ux-design-examples)
- Structured / Sunsama: [routine.co](https://routine.co/blog/posts/apps-like-structured), [akiflow.com](https://akiflow.com/blog/structured-vs-sunsama), [efficient.app](https://efficient.app/apps/sunsama)
- Notion / Bear / Obsidian: [zapier.com](https://zapier.com/blog/best-note-taking-apps/), [atlasworkspace.ai](https://www.atlasworkspace.ai/blog/best-note-taking-apps)
- Notion PARA method: [mariepoulin.com](https://mariepoulin.com/blog/using-para-to-organize-your-notion-workspace/), [notionmastery.com](https://notionmastery.com/using-the-para-method-with-notion/)
- Streaks / Spark (goal apps): [clickup.com](https://clickup.com/blog/goal-tracking-apps/), [ux-design-awards.com](https://ux-design-awards.com/winners/2024-2-spark-goal-tracking-app-with-gamification)
- Asana milestones/timeline: [forum.asana.com](https://forum.asana.com/t/milestones-in-asana-examples-best-practices/166038), [asana-design on Medium](https://medium.com/asana-design/designing-timeline-lessons-learned-from-our-journey-beyond-gantt-charts-645e80177aaa), [eleken.co timeline UI](https://www.eleken.co/blog-posts/timeline-ui-design)
- Forest / Toggl Track: [reclaim.ai](https://reclaim.ai/blog/best-pomodoro-timer-apps), [toggl.com](https://toggl.com/track/time-reporting/), [support.toggl.com list view](https://support.toggl.com/tracking-time-in-list-view)
- StoryGraph / Goodreads: [ixd.prattsi.org design critique](https://ixd.prattsi.org/2024/09/design-critique-storygraph-beats-goodreads-but-could-be-better/), [unstar.app](https://unstar.app/blog/goodreads-storygraph-fable-hardcover-bookly-reading-tracker-apps-ranked-2026)
- Paprika / Mealime: [paprikaapp.com](https://www.paprikaapp.com/), [foodieprep.ai](https://www.foodieprep.ai/blog/meal-planning-apps-in-2026-which-tools-actually-simplify-your-kitchen)
- Apple Shortcuts / IFTTT: [ifttt.com](https://ifttt.com/explore/guide-to-automation-ios-android)
- Raycast / Spotlight command palettes: [mobbin.com](https://mobbin.com/glossary/command-palette), [aiskill.market](https://aiskill.market/blog/inside-raycasts-design-system), [destiner.io](https://destiner.io/blog/post/designing-a-command-palette/)
- Settings UX: [developer.android.com](https://developer.android.com/design/ui/mobile/guides/patterns/settings), [setproduct.com](https://www.setproduct.com/blog/settings-ui-design), [netguru.com](https://www.netguru.com/blog/how-to-improve-app-settings-ux)
- Personal CRM (Clay/Dex): [getdex.com](https://getdex.com/blog/6-top-personal-crm-software-with-mobile-apps/), [yourpond.io](https://www.yourpond.io/blog/best-personal-crm-apps-2026)

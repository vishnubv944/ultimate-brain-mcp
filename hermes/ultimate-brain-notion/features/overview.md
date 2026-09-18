---
title: Ultimate Brain 3.0 — Full System Overview
purpose: Complete tour of all features and pages in Ultimate Brain 3.0 — what exists, how it connects, and how to navigate the system
agent_relevant: true
key_topics: [overview, homepage, tasks, notes, projects, tags, goals, people, books, recipes, my-day, my-week, my-year, gtd, archive, para, databases, quick-capture, flylighter, creators-companion]
related_docs: [features/managing-tasks.md, features/daily-planning.md, features/gtd-process.md, databases/tasks.md, databases/projects.md, databases/notes.md, databases/tags.md, databases/goals.md]
---

# Ultimate Brain 3.0 — Full System Overview

Ultimate Brain is an all-in-one personal productivity system built in Notion. It combines:
- Task management
- Note-taking and web clipping
- Project management
- Goal tracking with milestones
- Personal CRM (People)
- Book library and reading tracker
- Recipe tracker and meal planner
- Daily / Weekly / Yearly planning pages
- GTD (Getting Things Done) support

Everything is built around Notion's **side peek** mode — you can open and manage almost everything from the homepage without digging into sub-pages.

---

## The Homepage

The homepage is a single-page dashboard designed to cover 95% of daily use. It has sections for:

- **Tasks** — tabbed views (Today, Inbox, Week, Month, Scheduled, No Due, Recurring, Active Projects, Done)
- **Notes** — tabbed views (Notes, Clips, Inbox, Favorites, Voice, Journal, Meetings, All)
- **Projects** — tabbed views (Active, Planned, Board)
- **Tags** — tabbed views (By Type, Areas, Resources, All)
- **Goals** (optional widget from Dashboard Component Library)
- **People** (optional widget)
- **Books / Recipes** (optional widgets)

All section headers link to dedicated pages for that feature. The **Dashboard Component Library** (linked on the homepage) contains copy-pasteable widgets for sections not shown by default.

---

## Navigation

Three ways to navigate:
1. **Sidebar** — favorite the Ultimate Brain page to pin it; toggle open to see all sub-pages
2. **Global Nav Toggle** — click the nav element to see all top-level pages and quick-create buttons (New Task, New Note, New Project)
3. **Homepage section headers** — H1 links that go directly to dedicated pages

---

## Core Pages

### Quick Capture
Dedicated to fast capture with two clean inbox views:
- **Task Inbox Today** — only tasks created today; auto-clears yesterday's
- **Note Inbox Today** — same for notes
- Also has a clip inbox, project reference view, and done tasks view
- Ideal for phone home screen widgets

### Tasks Page
Full task manager with all layout types (List, Table, Board, Calendar) for:
- Inbox, Today, Next 7 Days, Next Month, All Tasks, Tasks by Project, Someday, Priority, Recurring

### Notes Page
Full note-taking system with:
- Note Inbox, Clip Inbox, Table, Favorites, By Tag, All Notes, Journal, Meeting Notes, Review Queue, Note Board

### Projects Page
Project manager with active/planned/board/calendar/completed views plus:
- By Tag (swim lane by Area)
- By People (swim lane by contact)

### Tags Page
PARA organization hub. Tags have three types:
- **Area** — ongoing sphere of life responsibility (Work, Health, Finance, Home). Gets the Area template with views for Projects, Notes, Web Clips, People, Goals, Sub-Tags.
- **Resource** — topical area of interest (Gardening, Programming, Photography). Gets Resource template with Notes and Web Clips views including a By Site grouping.
- **Entity** — meta-collection type (Essays, Apps, Papers). Describes the *shape* of content rather than its topic.

The recommended way to associate tasks with an Area is to create an **Ongoing Project** for each area and put maintenance tasks in it — rather than creating a direct Task → Tag relation (which would create ambiguity when processing tasks).

### Goals Page
Separate from Projects because goals are aspirational — you may not be able to fully plan the path to achieve them.

| Status | Meaning |
|---|---|
| `Dream` | Set but not actively pursuing |
| `Active` | Working toward it with projects underneath |
| `Achieved` | Done |

Each goal page has: overview, Milestones database view, associated Projects view. Milestones track measurable checkpoints toward the goal.

**Hierarchy:** Tags → Goals → Projects → Tasks

### Archive Page
Any page in Ultimate Brain can be archived by checking its `Archived` property. Archived items:
- Disappear from all main views
- Are safe in the Archive page (searchable)
- Can be unarchived at any time

Archive categories: Notes, Projects, Tags, Goals. Tasks use Done status instead.

### People Page (CRM)
Personal contact management. Each person page (using the Person template) shows:
- Meeting Notes related to them
- Notes related to them
- Projects they're involved in
- Tasks associated with them
- Gift ideas section
- Quick notes

Properties: Full Name, Relationship, Email, Phone, Company, Title, Birthday, Check-In, Last Check-In, Location, Website, LinkedIn, Twitter/X, Instagram, Interests, Pipeline Status.

**Pipeline Status** for sales tracking: Prospect → Contacted → Negotiating → Rejected/Closed.

**Birthday Calendar** — a formula calculates `Next Birthday` from the birthday property, viewable in Notion Calendar.

### Books Page
Full book library and reading tracker:
- Statuses: To Read, Reading, Read
- Properties: Author, Rating (1-5 stars), Genres, Pages, Publish Year, Owned Formats (Paperback/Hardback/Kindle/Audiobook), Shelves (Favorites, Reread-Worthy, etc.), Read Next flag
- **Reading Log** database tracks individual reading sessions with Start Page, End Page, Log Date
- Progress formula shows % of book read based on Reading Log entries
- Can add chart views to visualize reading habit over time (chart feature is Notion-plan-dependent)

### Recipes Page
Recipe tracker + meal planner:
- Recipe properties: Prep Time, Cook Time, Servings, Source URL, Recipe Tags
- `Is Quick` formula: marks any recipe ≤45 min as quick; shows in Quick view
- **Inbox property** on recipes: check it to hide a recipe from the main book while drafting
- **Meal Planner** database: connects recipes to specific dates and meal types (Breakfast/Lunch/Dinner/Snack/Other); shows in calendar view; opens in Notion Calendar
- **Recipe Tags** database: categorizes by Style (Cuisine type), Course (Appetizer/Entrée/Side), generic tags (Healthy, One Pan), and Equipment

### My Day Page
See [features/daily-planning.md](features/daily-planning.md). The most operationally important page — Plan / Execute / Wrap-Up workflow for daily work.

### My Week Page
Weekly reset and planning page with sections:
1. **Clear and Reset** — process inbox, handle overdue tasks, clear note inbox, update project statuses
2. **Reflect and Set Intent** — journal view, meeting notes review
3. **Plan the Week** — calendar, scheduled tasks, recurring tasks views

The "Clear and Reset" section is inspired by the Mise en place philosophy — keeping your workspace ordered so you can execute efficiently.

### My Year Page
Long-term planning page showing:
- **This Quarter** — goals and projects with Target Deadline in the current quarter
- **This Year** — goals and projects with Target Deadline in the current year

### Process (GTD) Page
See [features/gtd-process.md](features/gtd-process.md). Full GTD workflow: Task Intake, Note Processing, and all GTD list views (Do Next, Calendar, Delegated, Snoozed, Someday, Done). Optional — doesn't interfere with normal task management.

---

## Databases & Components Page

The "boiler room" of the template. All 14 source databases live here:

**Core:** Tasks, Projects, Notes, Tags, Goals, Milestones, People, Work Sessions
**Books:** Books, Reading Log, Genres
**Recipes:** Recipes, Recipe Tags, Meal Planner

All databases are **locked by default** (prevents accidental structural changes). Unlock to add properties, new status/select options, or edit formulas. Re-lock when done.

---

## Key Design Principles

### Side Peek First
Everything is designed to be opened in side peek from the homepage. You rarely need to navigate away.

### View Descriptions
Every database view has a hover description explaining what it shows and its filter logic.

### Property Descriptions
Every database property shows a description when you hover over it in a full-page view.

### PARA Architecture
Tags (Areas/Resources) → Goals → Projects → Tasks forms the PARA-inspired hierarchy. The Archive supports the A in PARA.

### Locked Databases
Databases ship locked to prevent accidental edits. This is intentional. Unlock before making structural changes; re-lock after.

---

## Web Clipping — Flylighter

Flylighter is a browser extension (Chrome/Chromium, Firefox coming) built by the same team as Ultimate Brain. Key features:
- Capture web pages, highlights, full articles, images to any Notion database
- Set ANY database property during capture (select, multi-select, relation, etc.)
- Auto-fill properties (e.g. always set Tag = "Productivity" for a specific flow)
- Capture LinkedIn profiles directly to the People database
- Multiple "flows" for different capture destinations

Web clips appear in Notes with `Type = "Web Clip"` and the source URL in the `URL` property.

---

## Creator's Companion Integration (Optional Bundle)

If you have the Ultimate Brain + Creator's Companion bundle, the integrated version adds:
- A Content database for content project management
- Notes database used as the Creator's Companion research database (notes gain Content and Keywords relation properties)
- Tasks grouped by Content in the task manager
- Active Content view on the homepage

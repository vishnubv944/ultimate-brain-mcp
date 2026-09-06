package com.example.data

import com.example.model.AcceptanceCriterion
import com.example.model.Priority
import com.example.model.Project
import com.example.model.SubTask
import com.example.model.Task
import com.example.model.TaskStatus

object DummyData {
  val initialProjects = listOf(
    Project("p-work-1", "Q3 launch — v2.0", "Doing", "62%"),
    Project("p-work-2", "Migrate auth to OAuth 2.1", "Doing", "40%"),
    Project("p-work-3", "Hire 2 senior engineers", "Ongoing", "50%"),
    Project("p-work-4", "Re-design settings screen", "Not Started", "0%"),
    Project("p-work-5", "Q2 retrospective", "Done", "100%")
  )

  val initialTasks = listOf(
    // 1. In Progress Active Focus Task (Hero item in Task Detail)
    Task(
      id = "tk-w-1",
      name = "Cut release branch and tag",
      status = TaskStatus.DOING,
      priority = Priority.HIGH,
      due = "2026-09-06",
      dueDisplay = "5:00 PM",
      timeBlock = "Today, Sep 6 · 10:00 AM – 5:00 PM",
      isMyDay = true,
      projectId = "p-work-1",
      projectName = "Q3 launch — v2.0",
      labels = listOf("Release", "Code"),
      timeTracked = "02:34:04",
      isActiveSession = true,
      taxonomyArea = "#Work (Area)",
      subTasksCount = 3,
      subTasks = listOf(
        SubTask("st-1", "Run automated CI smoke tests", isCompleted = false),
        SubTask("st-2", "Bump semantic version to v2.0.0", isCompleted = false),
        SubTask("st-3", "Publish changelog artifact", isCompleted = false)
      ),
      implementationNotes = "Ensure production smoke builds complete in pipeline before invoking tag operations. Tagging pushes immediately to staging mirrors.",
      codeSnippet = "git tag -a v2.0.0 -m \"Release v2.0.0\"",
      acceptanceCriteria = listOf(
        AcceptanceCriterion("ac-1", "Release branch named ", "release/v2.0", isChecked = true),
        AcceptanceCriterion("ac-2", "Artifact digests cryptographically signed with GPG", null, isChecked = false)
      )
    ),
    // 2. Today: Write release notes
    Task(
      id = "tk-w-2",
      name = "Write release notes",
      status = TaskStatus.TODO,
      priority = Priority.HIGH,
      due = "2026-09-06",
      dueDisplay = "Today",
      isMyDay = true,
      projectId = "p-work-1",
      projectName = "Q3 launch — v2.0",
      labels = listOf("Docs")
    ),
    // 3. Today: Review OAuth library PR
    Task(
      id = "tk-w-4",
      name = "Review OAuth library PR",
      status = TaskStatus.DOING,
      priority = Priority.HIGH,
      due = "2026-09-06",
      dueDisplay = "Today",
      isMyDay = true,
      projectId = "p-work-2",
      projectName = "Migrate auth to OAuth 2.1",
      labels = listOf("Code", "Review")
    ),
    // 4. Today: Review pull request: settings screen
    Task(
      id = "tk-w-pr-settings",
      name = "Review pull request: settings screen",
      status = TaskStatus.DOING,
      priority = Priority.MEDIUM,
      due = "2026-09-06",
      dueDisplay = "Today",
      isMyDay = true,
      projectId = "p-work-4",
      projectName = "Re-design settings screen",
      labels = emptyList(),
      subTasksCount = 3,
      subTasks = listOf(
        SubTask("st-s1", "Verify design tokens matching M3", isCompleted = false),
        SubTask("st-s2", "Test dark theme switch ripple", isCompleted = false),
        SubTask("st-s3", "Check accessibility touch targets", isCompleted = false)
      )
    ),
    // 5. Overdue Item (Needs triage alert card)
    Task(
      id = "tk-w-overdue-1",
      name = "Send Q2 retrospective summary",
      status = TaskStatus.TODO,
      priority = Priority.HIGH,
      due = "2026-09-04",
      dueDisplay = "Due Sep 4",
      isMyDay = false,
      projectId = "p-work-5",
      projectName = "Q2 retrospective",
      labels = listOf("Comms"),
      isOverdue = true
    ),
    // 6. Upcoming: Notify customer success team
    Task(
      id = "tk-w-upcoming-1",
      name = "Notify customer success team",
      status = TaskStatus.TODO,
      priority = Priority.MEDIUM,
      due = "2026-09-07",
      dueDisplay = "Tomorrow · Sep 7",
      isMyDay = false,
      projectId = "p-work-1",
      projectName = "Q3 launch — v2.0",
      labels = listOf("Comms")
    ),
    // 7. Upcoming: Schedule candidate interviews
    Task(
      id = "tk-w-upcoming-2",
      name = "Schedule candidate interviews",
      status = TaskStatus.TODO,
      priority = Priority.MEDIUM,
      due = "2026-09-10",
      dueDisplay = "Sep 10",
      isMyDay = false,
      projectId = "p-work-3",
      projectName = "Hire 2 senior engineers",
      labels = listOf("Hiring")
    ),
    // 8. To Do Habit / Recurring
    Task(
      id = "tk-w-habit-1",
      name = "Bottle clean",
      status = TaskStatus.TODO,
      priority = null,
      due = "2026-09-06",
      dueDisplay = "Today",
      isMyDay = true,
      projectName = null,
      labels = listOf("Habits"),
      isRecurring = true,
      recurrenceText = "Daily recurring"
    ),
    // 9. Done tasks for today (Completed bucket)
    Task(
      id = "tk-w-5",
      name = "Audit existing auth flows",
      status = TaskStatus.DONE,
      priority = Priority.MEDIUM,
      due = "2026-08-20",
      dueDisplay = "Done",
      isMyDay = false,
      projectId = "p-work-2",
      projectName = "Migrate auth to OAuth 2.1",
      labels = listOf("Research"),
      completionDate = "Today"
    ),
    Task(
      id = "tk-w-9-child-1",
      name = "Check accessibility audit results",
      status = TaskStatus.DONE,
      priority = Priority.MEDIUM,
      due = "2026-09-05",
      dueDisplay = "Done",
      isMyDay = false,
      projectId = "p-work-1",
      projectName = "Q3 launch — v2.0",
      labels = listOf("A11y"),
      completionDate = "Today"
    )
  )

  val projectsList = listOf(
    com.example.model.ProjectModel(
      id = "p-work-1",
      name = "Q3 launch — v2.0 release",
      status = "Doing",
      deadline = "Sep 30, 2026",
      progress = 0.62f,
      progressText = "62%",
      totalTasks = 12,
      doneTasks = 5,
      doingTasks = 3,
      todoTasks = 4,
      notesCount = 3,
      tags = listOf("#Work"),
      goalName = "Ship a stable v2.0 by EOY",
      templateName = "Product Release Template"
    ),
    com.example.model.ProjectModel(
      id = "p-work-2",
      name = "Migrate auth to OAuth 2.1",
      status = "Doing",
      deadline = "Oct 15, 2026",
      progress = 0.40f,
      progressText = "40%",
      totalTasks = 8,
      doneTasks = 1,
      doingTasks = 2,
      todoTasks = 5,
      notesCount = 1,
      tags = listOf("#Work", "#Engineering"),
      goalName = "Ship a stable v2.0 by EOY"
    ),
    com.example.model.ProjectModel(
      id = "p-work-3",
      name = "Hire 2 senior engineers",
      status = "Ongoing",
      deadline = "Dec 31, 2026",
      progress = 0.50f,
      progressText = "50%",
      totalTasks = 6,
      doneTasks = 3,
      doingTasks = 1,
      todoTasks = 2,
      notesCount = 0,
      tags = listOf("#Work")
    ),
    com.example.model.ProjectModel(
      id = "p-work-4",
      name = "Re-design settings screen",
      status = "Not Started",
      deadline = "Nov 15, 2026",
      progress = 0.0f,
      progressText = "0%",
      totalTasks = 5,
      doneTasks = 0,
      doingTasks = 0,
      todoTasks = 5,
      notesCount = 0,
      tags = listOf("#Work", "#Design")
    ),
    com.example.model.ProjectModel(
      id = "p-work-5",
      name = "Q2 retrospective",
      status = "Done",
      deadline = "Jul 15, 2026",
      progress = 1.0f,
      progressText = "100%",
      totalTasks = 4,
      doneTasks = 4,
      doingTasks = 0,
      todoTasks = 0,
      notesCount = 1,
      tags = listOf("#Work"),
      isArchived = true
    )
  )

  val notesList = listOf(
    com.example.model.NoteModel(
      id = "n-1",
      title = "Engineering all-hands — Sep 1",
      type = "Meeting",
      date = "Sep 1, 2026",
      projectName = "Q3 launch — v2.0",
      tags = listOf("#Work (Area)"),
      isFavorite = true,
      excerpt = "v2.0 is on track for Sep 30. Action: cut release branch by Sep 6. Staging verification underway with QA team...",
      attendees = listOf("Priya", "Anil", "Maya", "Devraj", "Tomás"),
      agenda = listOf("Q3 progress review", "v2.0 release readiness", "Hiring update"),
      noteBullets = listOf(
        Pair("Action:", "cut release branch by Sep 6."),
        Pair("Action:", "schedule onsites."),
        Pair("Action:", "Tomás to review by Sep 8.")
      ),
      decisions = "Push v2.0 to Oct 7 if auth migration slips beyond Sep 30.",
      actionItems = listOf(
        com.example.model.NoteActionItem("ai-1", "Schedule follow-up sync with QA lead", false),
        com.example.model.NoteActionItem("ai-2", "Send meeting summary to Slack #eng-general", true)
      ),
      rawMarkdown = """## Attendees
- Priya, Anil, Maya, Devraj, Tomás

## Agenda
1. Q3 progress review
2. v2.0 release readiness
3. Hiring update

## Notes
- v2.0 is on track for Sep 30. **Action:** cut release branch by Sep 6.
- Hiring pipeline has 8 candidates, 2 strong. **Action:** schedule onsites.
- Auth migration blocked on library PR review. **Action:** Tomás to review by Sep 8.

## Decisions
> Push v2.0 to Oct 7 if auth migration slips beyond Sep 30.

## Action Items
- [ ] Schedule follow-up sync with QA lead
- [x] Send meeting summary to Slack #eng-general"""
    ),
    com.example.model.NoteModel(
      id = "n-2",
      title = "OAuth library comparison",
      type = "Reference",
      date = "Aug 15, 2026",
      projectName = "Migrate auth to OAuth 2.1",
      tags = listOf("#Work", "#Engineering"),
      isFavorite = false,
      excerpt = "Libraries considered: Authlib, oslo, oauthlib. Benchmarks indicate Authlib provides minimal latency for token verification...",
      rawMarkdown = """## OAuth Libraries
- Authlib: Best compliance and fast token validation.
- oslo: High memory overhead.
- oauthlib: Lacks PKCE RFC 7636 support out of the box."""
    ),
    com.example.model.NoteModel(
      id = "n-3",
      title = "Idea: customer-facing changelog",
      type = "Idea",
      date = "Aug 28, 2026",
      projectName = "Dev Experience",
      tags = listOf("#Work"),
      isFavorite = false,
      excerpt = "Problem: Customers complain about silent changes. Idea: A /changelog page auto-built from GitHub releases, with categories for enhancements, fixes, and API updates...",
      rawMarkdown = """## Problem
Customers don't know what shipped in patch updates.

## Solution
Auto-generate /changelog using GitHub releases webhook."""
    ),
    com.example.model.NoteModel(
      id = "n-4",
      title = "Morning pages — Sep 5",
      type = "Journal",
      date = "Sep 5, 2026",
      projectName = null,
      tags = listOf("#Personal"),
      isFavorite = true,
      excerpt = "Three pages, freewrite. The house was quiet. Coffee tasted better today — Sam brought a new bag from the roaster downtown...",
      rawMarkdown = """Three pages, freewrite. The house was quiet. Coffee tasted better today — Sam brought a new bag from the roaster downtown.

I keep returning to the same question: what would I do if I knew I couldn't fail? The honest answer is that I'd write."""
    ),
    com.example.model.NoteModel(
      id = "n-5",
      title = "Atomic Habits — Chapter 4 key ideas",
      type = "Book",
      date = "Aug 20, 2026",
      projectName = "Read 24 books this year",
      tags = listOf("#Learning"),
      isFavorite = false,
      excerpt = "The Four Laws of Behavior Change: 1. Cue — make it obvious, 2. Craving — make it attractive, 3. Response — make it easy, 4. Reward — make it satisfying...",
      rawMarkdown = """## The Four Laws
1. Cue — make it obvious
2. Craving — make it attractive
3. Response — make it easy
4. Reward — make it satisfying"""
    )
  )

  val goalsList = listOf(
    com.example.model.GoalModel(
      id = "g-1",
      name = "Ship a stable v2.0 by EOY",
      status = "Active",
      deadline = "Dec 31, 2026",
      daysRemaining = 116,
      tagArea = "Work (Area)",
      aggregatedProgress = 0.51f,
      aggregatedProgressText = "51%",
      totalTasks = 20,
      closedTasks = 6,
      completedMilestonesCount = 2,
      totalMilestonesCount = 3,
      linkedProjects = listOf(
        com.example.model.GoalProjectSummary("p-work-1", "Q3 launch — v2.0 release", "Sep 30, 2026", "Doing", 0.62f, "62%", "12 tasks (5 done, 3 doing, 4 todo)"),
        com.example.model.GoalProjectSummary("p-work-2", "Migrate auth to OAuth 2.1", "Oct 15, 2026", "Doing", 0.40f, "40%", "8 tasks (1 done, 2 doing, 5 todo)")
      )
    ),
    com.example.model.GoalModel(
      id = "g-2",
      name = "Be fit enough to run 13.1 miles",
      status = "Active",
      deadline = "Dec 15, 2026",
      daysRemaining = 100,
      tagArea = "Health",
      aggregatedProgress = 0.45f,
      aggregatedProgressText = "45%",
      totalTasks = 8,
      closedTasks = 3,
      completedMilestonesCount = 1,
      totalMilestonesCount = 2,
      linkedProjects = listOf(
        com.example.model.GoalProjectSummary("p-health-1", "Run a half marathon", "Dec 15, 2026", "Doing", 0.45f, "45%", "8 tasks (3 done, 1 doing, 4 todo)")
      )
    ),
    com.example.model.GoalModel(
      id = "g-3",
      name = "Read widely and deeply",
      status = "Active",
      deadline = "Dec 31, 2026",
      daysRemaining = 116,
      tagArea = "Learning",
      aggregatedProgress = 0.58f,
      aggregatedProgressText = "58%",
      totalTasks = 24,
      closedTasks = 13,
      completedMilestonesCount = 0,
      totalMilestonesCount = 1,
      linkedProjects = listOf(
        com.example.model.GoalProjectSummary("p-learn-1", "Read 24 books this year", "Dec 31, 2026", "Ongoing", 0.58f, "58%", "14 of 24 books completed")
      )
    ),
    com.example.model.GoalModel(
      id = "g-4",
      name = "Quit social media",
      status = "Achieved",
      deadline = "May 15, 2024",
      daysRemaining = 0,
      tagArea = "Lifestyle",
      aggregatedProgress = 1.0f,
      aggregatedProgressText = "100%",
      totalTasks = 5,
      closedTasks = 5,
      completionDate = "May 15, 2024"
    )
  )

  val milestonesList = listOf(
    com.example.model.MilestoneModel(
      id = "ms-1",
      name = "API contracts frozen & reviewed",
      goalId = "g-1",
      goalName = "Ship a stable v2.0 by EOY",
      goalCategory = "Engineering",
      status = "Completed",
      targetDateText = "Delivered Aug 20, 2026 · Target was Aug 25",
      linkedTasksDone = 6,
      linkedTasksTotal = 6,
      note = "Finished 5d early"
    ),
    com.example.model.MilestoneModel(
      id = "ms-2",
      name = "Release candidate branch cut",
      goalId = "g-1",
      goalName = "Ship a stable v2.0 by EOY",
      goalCategory = "Engineering",
      status = "In Progress",
      targetDateText = "Target: Today, Sep 6",
      linkedTasksDone = 3,
      linkedTasksTotal = 4,
      isToday = true,
      note = "Active work session linked"
    ),
    com.example.model.MilestoneModel(
      id = "ms-3",
      name = "Zero P0 bugs for 7 days",
      goalId = "g-1",
      goalName = "Ship a stable v2.0 by EOY",
      goalCategory = "Engineering",
      status = "Pending",
      targetDateText = "Target: Sep 25, 2026",
      linkedTasksDone = 0,
      linkedTasksTotal = 5,
      note = "Starts after RC",
      checklistItemsCount = 5
    ),
    com.example.model.MilestoneModel(
      id = "ms-4",
      name = "Run first 10K without stopping",
      goalId = "g-2",
      goalName = "Be fit enough to run 13.1 miles",
      goalCategory = "Health",
      status = "Completed",
      targetDateText = "Completed Aug 10, 2026 · Pace 5:45/km",
      linkedTasksDone = 1,
      linkedTasksTotal = 1
    ),
    com.example.model.MilestoneModel(
      id = "ms-5",
      name = "Complete 15K long run",
      goalId = "g-2",
      goalName = "Be fit enough to run 13.1 miles",
      goalCategory = "Health",
      status = "In Progress",
      targetDateText = "Target: Oct 1, 2026 · Week 7 Training Block",
      linkedTasksDone = 12,
      linkedTasksTotal = 15
    ),
    com.example.model.MilestoneModel(
      id = "ms-6",
      name = "Reach $5K emergency fund",
      goalId = "g-3",
      goalName = "Build emergency fund (3 months)",
      goalCategory = "Finance",
      status = "Completed",
      targetDateText = "Completed Jul 1, 2026 · Automated savings rule active",
      linkedTasksDone = 4,
      linkedTasksTotal = 4
    )
  )

  val tagsList = listOf(
    com.example.model.TagModel(
      id = "t-area-1",
      name = "#Work",
      type = "Area",
      icon = "corporate_fare",
      isFavorite = true,
      totalItems = 18,
      activeItems = 8,
      projectsCount = 4,
      notesCount = 6,
      children = listOf(
        com.example.model.TagHierarchyItem("t-sub-1", "Engineering", 10),
        com.example.model.TagHierarchyItem("t-sub-2", "Design", 5),
        com.example.model.TagHierarchyItem("t-sub-3", "Hiring", 3)
      )
    ),
    com.example.model.TagModel(
      id = "t-area-2",
      name = "#Health",
      type = "Area",
      icon = "favorite",
      isFavorite = false,
      totalItems = 6,
      activeItems = 3,
      projectsCount = 1,
      notesCount = 2,
      children = listOf(
        com.example.model.TagHierarchyItem("t-sub-4", "Running", 4),
        com.example.model.TagHierarchyItem("t-sub-5", "Meditation", 2)
      )
    ),
    com.example.model.TagModel(
      id = "t-area-3",
      name = "#Finance",
      type = "Area",
      icon = "payments",
      isFavorite = false,
      totalItems = 3,
      activeItems = 1,
      projectsCount = 1,
      notesCount = 0
    ),
    com.example.model.TagModel(
      id = "t-res-1",
      name = "Engineering",
      type = "Resource",
      icon = "terminal",
      isFavorite = false,
      totalItems = 9,
      activeItems = 5,
      projectsCount = 2,
      notesCount = 3,
      children = listOf(
        com.example.model.TagHierarchyItem("t-sub-6", "Architecture RFC", 5),
        com.example.model.TagHierarchyItem("t-sub-7", "OAuth 2.1", 4)
      )
    ),
    com.example.model.TagModel(
      id = "t-res-2",
      name = "Design",
      type = "Resource",
      icon = "palette",
      isFavorite = false,
      totalItems = 7,
      activeItems = 4,
      projectsCount = 1,
      notesCount = 2,
      children = listOf(
        com.example.model.TagHierarchyItem("t-sub-8", "UI Polish", 4),
        com.example.model.TagHierarchyItem("t-sub-9", "Accessibility", 3)
      )
    ),
    com.example.model.TagModel(
      id = "t-res-3",
      name = "Productivity",
      type = "Resource",
      icon = "bolt",
      isFavorite = false,
      totalItems = 4,
      activeItems = 2,
      projectsCount = 1,
      notesCount = 1
    ),
    com.example.model.TagModel(
      id = "t-ent-1",
      name = "Priya (me)",
      type = "Entity",
      icon = "person",
      isFavorite = true,
      totalItems = 24,
      activeItems = 12,
      projectsCount = 4,
      notesCount = 8
    ),
    com.example.model.TagModel(
      id = "t-ent-2",
      name = "Tomás",
      type = "Entity",
      icon = "person",
      isFavorite = false,
      totalItems = 5,
      activeItems = 3,
      projectsCount = 1,
      notesCount = 1
    ),
    com.example.model.TagModel(
      id = "t-ent-3",
      name = "Sarah Chen",
      type = "Entity",
      icon = "person",
      isFavorite = false,
      totalItems = 3,
      activeItems = 1,
      projectsCount = 1,
      notesCount = 1
    )
  )

  val workSessionsList = listOf(
    com.example.model.WorkSessionModel(
      id = "ws-1",
      taskName = "Cut release branch and tag",
      taskId = "tk-w-1",
      projectName = "Q3 launch — v2.0",
      timeRange = "Started 10:00 AM",
      duration = "02:34:04",
      isRunning = true,
      isToday = true
    ),
    com.example.model.WorkSessionModel(
      id = "ws-2",
      taskName = "Review OAuth library PR",
      taskId = "tk-w-3",
      projectName = "Migrate auth",
      timeRange = "8:30 AM – 9:45 AM",
      duration = "01:15:00",
      isRunning = false,
      isToday = true
    ),
    com.example.model.WorkSessionModel(
      id = "ws-3",
      taskName = "Audit existing auth flows",
      taskId = "tk-w-5",
      projectName = "Migrate auth",
      timeRange = "2:00 PM – 4:45 PM",
      duration = "02:45:00",
      isRunning = false,
      isToday = false
    ),
    com.example.model.WorkSessionModel(
      id = "ws-4",
      taskName = "Settings IA wireframing",
      taskId = "tk-w-4",
      projectName = "Core UX Refactor",
      timeRange = "10:00 AM – 11:20 AM",
      duration = "01:20:00",
      isRunning = false,
      isToday = false
    )
  )
}


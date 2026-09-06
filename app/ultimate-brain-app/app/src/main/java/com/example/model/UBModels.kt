package com.example.model

// --- Projects ---
data class ProjectModel(
  val id: String,
  val name: String,
  val status: String = "Doing", // Not Started, Doing, Ongoing, Done
  val deadline: String = "Sep 30, 2026",
  val progress: Float = 0.62f,
  val progressText: String = "62%",
  val totalTasks: Int = 12,
  val doneTasks: Int = 5,
  val doingTasks: Int = 3,
  val todoTasks: Int = 4,
  val notesCount: Int = 3,
  val tags: List<String> = listOf("#Work"),
  val goalName: String? = "Ship a stable v2.0 by EOY",
  val templateName: String? = "Product Release Template",
  val isArchived: Boolean = false
)

// --- Notes ---
data class NoteActionItem(
  val id: String,
  val text: String,
  val isDone: Boolean = false
)

data class NoteModel(
  val id: String,
  val title: String,
  val type: String = "Meeting", // Meeting, Reference, Idea, Journal, Book, Recipe, etc.
  val date: String = "Sep 1, 2026",
  val projectName: String? = "Q3 launch — v2.0",
  val tags: List<String> = listOf("#Work (Area)"),
  val isFavorite: Boolean = false,
  val excerpt: String = "",
  val attendees: List<String> = emptyList(),
  val agenda: List<String> = emptyList(),
  val noteBullets: List<Pair<String, String>> = emptyList(), // Pair of action tag ("Action:"), note text
  val decisions: String? = null,
  val actionItems: List<NoteActionItem> = emptyList(),
  val rawMarkdown: String = ""
)

// --- Goals ---
data class GoalProjectSummary(
  val id: String,
  val name: String,
  val deadline: String,
  val status: String,
  val progress: Float,
  val progressText: String,
  val tasksSummary: String
)

data class GoalModel(
  val id: String,
  val name: String,
  val status: String = "Active", // Active, Achieved, Dropped
  val deadline: String = "Dec 31, 2026",
  val daysRemaining: Int = 116,
  val tagArea: String = "Work (Area)",
  val aggregatedProgress: Float = 0.51f,
  val aggregatedProgressText: String = "51%",
  val totalTasks: Int = 20,
  val closedTasks: Int = 6,
  val completedMilestonesCount: Int = 2,
  val totalMilestonesCount: Int = 3,
  val linkedProjects: List<GoalProjectSummary> = emptyList(),
  val completionDate: String? = null
)

// --- Milestones ---
data class MilestoneModel(
  val id: String,
  val name: String,
  val goalId: String,
  val goalName: String,
  val goalCategory: String, // Engineering, Health, Finance
  val status: String = "In Progress", // Completed, In Progress, Pending
  val targetDateText: String = "Target: Today, Sep 6",
  val linkedTasksDone: Int = 3,
  val linkedTasksTotal: Int = 4,
  val isToday: Boolean = false,
  val note: String? = null,
  val checklistItemsCount: Int = 0
)

// --- Tags ---
data class TagHierarchyItem(
  val id: String,
  val name: String,
  val itemsCount: Int = 0,
  val icon: String? = null
)

data class TagModel(
  val id: String,
  val name: String,
  val type: String = "Area", // Area, Resource, Entity
  val icon: String = "corporate_fare",
  val isFavorite: Boolean = false,
  val totalItems: Int = 18,
  val activeItems: Int = 8,
  val projectsCount: Int = 4,
  val notesCount: Int = 6,
  val children: List<TagHierarchyItem> = emptyList()
)

// --- Work Sessions ---
data class WorkSessionModel(
  val id: String,
  val taskName: String,
  val taskId: String,
  val projectName: String,
  val timeRange: String,
  val duration: String,
  val isRunning: Boolean = false,
  val isToday: Boolean = true
)

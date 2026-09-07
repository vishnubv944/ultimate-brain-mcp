package com.example.model

// --- Projects ---
data class ProjectModel(
  val id: String,
  val name: String,
  val status: String = "Doing", // Planned, On Hold, Doing, Ongoing, Done
  val deadline: String = "Sep 30, 2026",
  val deadlineIso: String? = null,
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
  val isArchived: Boolean = false,
  val reviewNotes: String = "",
  val tagIds: List<String> = emptyList(),
  val personIds: List<String> = emptyList(),
)

// --- Notes ---
data class NoteModel(
  val id: String,
  val title: String,
  val type: String = "Meeting", // Meeting, Reference, Idea, Journal, Book, Recipe, etc.
  val date: String = "Sep 1, 2026",
  val dateIso: String? = null,
  val projectName: String? = "Q3 launch — v2.0",
  val tags: List<String> = listOf("#Work (Area)"),
  val isFavorite: Boolean = false,
  val excerpt: String = "",
  val rawMarkdown: String = "",
  val url: String = "",
  val reviewDateIso: String? = null,
  val tagIds: List<String> = emptyList(),
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
  val status: String = "Active", // Dream, Active, Achieved
  val isArchived: Boolean = false,
  val deadline: String = "—",
  val deadlineIso: String? = null,
  val daysRemaining: Int = 0,
  val tagArea: String = "",
  val aggregatedProgress: Float = 0f,
  val aggregatedProgressText: String = "0%",
  val totalTasks: Int = 0,
  val closedTasks: Int = 0,
  val completedMilestonesCount: Int = 0,
  val totalMilestonesCount: Int = 0,
  val linkedProjects: List<GoalProjectSummary> = emptyList(),
  val completionDate: String? = null,
  val goalSetIso: String? = null,
  val tagId: String? = null,
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
  val targetDateIso: String? = null,
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
  val parentId: String? = null,
  val parentName: String? = null,
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
  val isToday: Boolean = true,
  val startIso: String? = null,
  val endIso: String? = null,
  val durationMinutes: Int? = null,
)

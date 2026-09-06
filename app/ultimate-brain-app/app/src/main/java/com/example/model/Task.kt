package com.example.model

enum class TaskStatus {
  TODO,
  DOING,
  DONE
}

enum class Priority {
  HIGH,
  MEDIUM,
  LOW
}

data class SubTask(
  val id: String,
  val name: String,
  val isCompleted: Boolean = false
)

data class Task(
  val id: String,
  val name: String,
  val status: TaskStatus = TaskStatus.TODO,
  val priority: Priority? = null,
  val due: String? = null,
  val dueDisplay: String = "Today",
  val isMyDay: Boolean = true,
  val projectId: String? = null,
  val projectName: String? = null,
  val labels: List<String> = emptyList(),
  val isRecurring: Boolean = false,
  val recurrenceText: String? = null,
  val timeTracked: String? = null,
  val isActiveSession: Boolean = false,
  val isOverdue: Boolean = false,
  val isDone: Boolean = (status == TaskStatus.DONE),
  val completionDate: String? = null,
  val parentTaskId: String? = null,
  val subTasksCount: Int = 0,
  val timeBlock: String? = null,
  val taxonomyArea: String? = null,
  val subTasks: List<SubTask> = emptyList(),
  // Extended Notion properties.
  val description: String = "",
  val energy: String? = null,       // High | Low
  val location: String? = null,     // Home | Office | Errand
  val smartList: String? = null,    // Do Next | Delegated | Someday
  val dueEndIso: String? = null,    // for time-blocked ranges
  val snoozeIso: String? = null,
)

data class Project(
  val id: String,
  val name: String,
  val status: String = "Doing",
  val progress: String = "0%"
)

enum class DailyRitualPhase(val label: String, val stepNumber: Int) {
  PLAN("1. Plan", 1),
  EXECUTE("2. Execute", 2),
  WRAP_UP("3. Wrap Up", 3)
}


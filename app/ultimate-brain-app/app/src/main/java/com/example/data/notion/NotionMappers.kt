package com.example.data.notion

import com.example.data.DateUtils
import com.example.model.GoalModel
import com.example.model.GoalProjectSummary
import com.example.model.MilestoneModel
import com.example.model.NoteModel
import com.example.model.Priority
import com.example.model.ProjectModel
import com.example.model.TagModel
import com.example.model.Task
import com.example.model.TaskStatus
import java.time.LocalDate

/**
 * Notion page JSON -> app model. Property names mirror `formatters.py`.
 *
 * Lookups ({id -> name}) resolve relation targets and are built by the
 * repository once all data sources have been fetched.
 */
object NotionMappers {

  fun toTask(
    page: NotionPage,
    projectNames: Map<String, String>,
    tagNames: Map<String, String>,
    today: LocalDate = LocalDate.now(),
  ): Task {
    val p = page.properties
    val statusName = p.prop("Status")?.selectName()
    val status = when (statusName) {
      "Doing" -> TaskStatus.DOING
      "Done" -> TaskStatus.DONE
      else -> TaskStatus.TODO
    }
    val rawDue = p.prop("Due")?.dateStart()
    val dueIso = rawDue?.substringBefore('T')
    val dueEnd = p.prop("Due")?.date?.end
    val projectIds = p.prop("Project").rel()
    val tagIds = p.prop("Tag", "Tags").rel()
    val recurUnit = p.prop("Recur Unit")?.selectName()
    val recurInterval = p.prop("Recur Interval")?.number?.toInt() ?: 1
    val bucket = DateUtils.bucket(dueIso, today)

    return Task(
      id = page.id,
      name = p.prop("Name")?.plainTitle().orEmpty(),
      status = status,
      priority = when (p.prop("Priority")?.selectName()) {
        "High" -> Priority.HIGH
        "Medium" -> Priority.MEDIUM
        "Low" -> Priority.LOW
        else -> null
      },
      due = dueIso,
      dueDisplay = DateUtils.displayLabel(dueIso, today).ifBlank { "Today" },
      isMyDay = p.prop("My Day")?.isChecked() == true,
      projectId = projectIds.firstOrNull(),
      projectName = projectIds.firstNotNullOfOrNull { projectNames[it] },
      labels = p.prop("Labels")?.multiNames().orEmpty(),
      isRecurring = recurUnit != null,
      recurrenceText = recurUnit?.let { "every $recurInterval $it" },
      timeTracked = p.prop("Time Tracked")?.formulaValue()?.toString(),
      isOverdue = bucket == DateUtils.DueBucket.OVERDUE && status != TaskStatus.DONE,
      isDone = status == TaskStatus.DONE,
      completionDate = p.prop("Completed")?.dateStart(),
      parentTaskId = p.prop("Parent Task").rel().firstOrNull(),
      timeBlock = if (rawDue != null && dueEnd != null) formatRange(rawDue, dueEnd) else null,
      taxonomyArea = tagIds.firstNotNullOfOrNull { tagNames[it] },
      description = p.prop("Description")?.plainTitle().orEmpty(),
      energy = p.prop("Energy")?.selectName(),
      location = p.prop("Location")?.selectName(),
      smartList = p.prop("Smart List")?.selectName(),
      dueEndIso = dueEnd,
      snoozeIso = p.prop("Snooze")?.dateStart(),
    )
  }

  fun toProject(
    page: NotionPage,
    goalNames: Map<String, String>,
    tagNames: Map<String, String>,
    tasksByProject: Map<String, List<Task>>,
  ): ProjectModel {
    val p = page.properties
    val id = page.id
    val goalIds = p.prop("Goal", "Goals").rel()
    val tagIds = p.prop("Tag", "Tags").rel()
    val tasks = tasksByProject[id].orEmpty()
    val done = tasks.count { it.status == TaskStatus.DONE }
    val doing = tasks.count { it.status == TaskStatus.DOING }
    val todo = tasks.count { it.status == TaskStatus.TODO }
    val formulaProgress = (p.prop("Progress")?.formulaValue() as? Number)?.toFloat()
    val progress = formulaProgress ?: if (tasks.isNotEmpty()) done.toFloat() / tasks.size else 0f

    return ProjectModel(
      id = id,
      name = p.prop("Name")?.plainTitle().orEmpty(),
      status = p.prop("Status")?.selectName() ?: "Doing",
      deadline = DateUtils.displayLabel(p.prop("Target Deadline")?.dateStart()).ifBlank { "—" },
      progress = progress.coerceIn(0f, 1f),
      progressText = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
      totalTasks = tasks.size,
      doneTasks = done,
      doingTasks = doing,
      todoTasks = todo,
      notesCount = 0,
      tags = tagIds.mapNotNull { tagNames[it] }.map { "#$it" }.ifEmpty { emptyList() },
      goalName = goalIds.firstNotNullOfOrNull { goalNames[it] },
      templateName = null,
      isArchived = p.prop("Archived")?.isChecked() == true,
    )
  }

  fun toNote(
    page: NotionPage,
    projectNames: Map<String, String>,
    tagNames: Map<String, String>,
  ): NoteModel {
    val p = page.properties
    val projectIds = p.prop("Project").rel()
    val tagIds = p.prop("Tag", "Tags").rel()
    return NoteModel(
      id = page.id,
      title = p.prop("Name")?.plainTitle().orEmpty(),
      type = p.prop("Type")?.selectName() ?: "Note",
      date = DateUtils.displayLabel(p.prop("Note Date")?.dateStart()).ifBlank { "—" },
      projectName = projectIds.firstNotNullOfOrNull { projectNames[it] },
      tags = tagIds.mapNotNull { tagNames[it] }.map { "#$it" },
      isFavorite = p.prop("Favorite")?.isChecked() == true,
      excerpt = "",
      rawMarkdown = "",
    )
  }

  fun toGoal(
    page: NotionPage,
    projectsById: Map<String, ProjectModel>,
    tagNames: Map<String, String>,
  ): GoalModel {
    val p = page.properties
    val projectIds = p.prop("Projects", "Project").rel()
    val linked = projectIds.mapNotNull { projectsById[it] }
    val totalMs = linked.size.coerceAtLeast(1)
    val doneMs = linked.count { it.status == "Done" }
    val agg = if (linked.isNotEmpty()) linked.map { it.progress }.average().toFloat() else 0f
    val deadlineIso = p.prop("Target Deadline")?.dateStart()

    return GoalModel(
      id = page.id,
      name = p.prop("Name")?.plainTitle().orEmpty(),
      status = p.prop("Status")?.selectName() ?: "Active",
      isArchived = p.prop("Archived")?.isChecked() == true,
      deadline = DateUtils.displayLabel(deadlineIso).ifBlank { "—" },
      daysRemaining = daysUntil(deadlineIso),
      tagArea = p.prop("Tag").rel().firstNotNullOfOrNull { tagNames[it] } ?: "",
      aggregatedProgress = agg.coerceIn(0f, 1f),
      aggregatedProgressText = "${(agg.coerceIn(0f, 1f) * 100).toInt()}%",
      totalTasks = linked.sumOf { it.totalTasks },
      closedTasks = linked.sumOf { it.doneTasks },
      completedMilestonesCount = doneMs,
      totalMilestonesCount = totalMs,
      linkedProjects = linked.map {
        GoalProjectSummary(
          id = it.id,
          name = it.name,
          deadline = it.deadline,
          status = it.status,
          progress = it.progress,
          progressText = it.progressText,
          tasksSummary = "${it.doneTasks}/${it.totalTasks} tasks",
        )
      },
      completionDate = p.prop("Achieved")?.dateStart(),
    )
  }

  fun toMilestone(
    page: NotionPage,
    goalNames: Map<String, String>,
    today: LocalDate = LocalDate.now(),
  ): MilestoneModel {
    val p = page.properties
    val goalId = p.prop("Goal", "Goals", "Related Goal").rel().firstOrNull()
    val completed = p.prop("Date Completed")?.dateStart()
    val deadlineIso = p.prop("Target Deadline")?.dateStart()?.substringBefore('T')
    val deadline = DateUtils.parseIsoDate(deadlineIso)
    val status = when {
      completed != null -> "Completed"
      deadline != null && !deadline.isAfter(today) -> "In Progress"
      else -> "Pending"
    }
    return MilestoneModel(
      id = page.id,
      name = p.prop("Name")?.plainTitle().orEmpty(),
      goalId = goalId ?: "",
      goalName = goalId?.let { goalNames[it] } ?: "",
      goalCategory = "",
      status = status,
      targetDateText = deadlineIso?.let { "Target: ${DateUtils.displayLabel(it)}" } ?: "",
      isToday = deadline == today,
    )
  }

  fun toTag(page: NotionPage): TagModel {
    val p = page.properties
    return TagModel(
      id = page.id,
      name = p.prop("Name")?.plainTitle().orEmpty(),
      type = p.prop("Type")?.selectName() ?: "Area",
      isFavorite = p.prop("Favorite")?.isChecked() == true,
    )
  }

  fun toPerson(page: NotionPage): com.example.model.PersonModel {
    val p = page.properties
    fun rt(k: String) = p.prop(k)?.plainTitle().orEmpty()
    return com.example.model.PersonModel(
      id = page.id,
      name = p.prop("Full Name", "Name")?.plainTitle().orEmpty(),
      company = rt("Company"),
      title = rt("Title"),
      email = p.prop("Email")?.email.orEmpty(),
      phone = p.prop("Phone")?.phoneNumber.orEmpty(),
      relationship = p.prop("Relationship")?.multiNames().orEmpty(),
      pipelineStatus = p.prop("Pipeline Status")?.selectName(),
      birthday = p.prop("Birthday")?.dateStart(),
      lastCheckIn = p.prop("Last Check-In")?.dateStart(),
      linkedIn = p.prop("LinkedIn")?.url.orEmpty(),
      twitter = p.prop("Twitter/X")?.url.orEmpty(),
      website = p.prop("Website")?.url.orEmpty(),
      location = rt("Location"),
    )
  }

  fun toBook(page: NotionPage): com.example.model.BookModel {
    val p = page.properties
    return com.example.model.BookModel(
      id = page.id,
      title = p.prop("Title", "Name")?.plainTitle().orEmpty(),
      author = p.prop("Author")?.plainTitle().orEmpty(),
      status = p.prop("Status")?.selectName() ?: "Want to Read",
      rating = p.prop("Rating")?.selectName(),
      pages = p.prop("Pages")?.number?.toInt(),
      publishYear = p.prop("Publish Year")?.number?.toInt(),
      dateStarted = p.prop("Date Started")?.dateStart(),
      dateFinished = p.prop("Date Finished")?.dateStart(),
      ownedFormats = p.prop("Owned Formats")?.multiNames().orEmpty(),
      shelf = p.prop("Shelf")?.multiNames().orEmpty(),
      readNext = p.prop("Read Next")?.isChecked() == true,
      description = p.prop("Description")?.plainTitle().orEmpty(),
      genreIds = p.prop("Genres").rel(),
    )
  }

  fun toReadingLog(page: NotionPage, bookTitles: Map<String, String>): com.example.model.ReadingLogModel {
    val p = page.properties
    val bookId = p.prop("Book").rel().firstOrNull()
    return com.example.model.ReadingLogModel(
      id = page.id,
      name = p.prop("Name")?.plainTitle().orEmpty(),
      bookId = bookId,
      bookTitle = bookId?.let { bookTitles[it] },
      logDate = p.prop("Log Date")?.dateStart(),
      startPage = p.prop("Start Page")?.number?.toInt(),
      endPage = p.prop("End Page")?.number?.toInt(),
    )
  }

  fun toGenre(page: NotionPage): com.example.model.GenreModel = com.example.model.GenreModel(
    id = page.id,
    name = page.properties.prop("Name")?.plainTitle().orEmpty(),
    bookCount = page.properties.prop("Books").rel().size,
  )

  fun toRecipe(page: NotionPage): com.example.model.RecipeModel {
    val p = page.properties
    return com.example.model.RecipeModel(
      id = page.id,
      name = p.prop("Name")?.plainTitle().orEmpty(),
      chef = p.prop("Chef Name")?.plainTitle().orEmpty(),
      prepTime = p.prop("Prep Time")?.number?.toInt(),
      cookTime = p.prop("Cook Time")?.number?.toInt(),
      servings = p.prop("Servings")?.number?.toInt(),
      mealTimes = p.prop("Meal Time")?.multiNames().orEmpty(),
      favorite = p.prop("Favorite")?.isChecked() == true,
      url = p.prop("URL")?.url.orEmpty(),
    )
  }

  fun toMealPlan(page: NotionPage, recipeNames: Map<String, String>): com.example.model.MealPlanModel {
    val p = page.properties
    val rids = p.prop("Recipes").rel()
    return com.example.model.MealPlanModel(
      id = page.id,
      name = p.prop("Name")?.plainTitle().orEmpty(),
      date = p.prop("Date")?.dateStart(),
      meal = p.prop("Meal")?.selectName(),
      recipeIds = rids,
      recipeNames = rids.mapNotNull { recipeNames[it] },
      favorite = p.prop("Favorite")?.isChecked() == true,
    )
  }

  // --- helpers ---

  private fun NotionProperty?.rel(): List<String> = this?.relationIds().orEmpty()

  private fun daysUntil(iso: String?): Int {
    val d = DateUtils.parseIsoDate(iso?.substringBefore('T')) ?: return 0
    return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), d).toInt().coerceAtLeast(0)
  }

  private fun formatRange(startIso: String, endIso: String): String {
    fun t(s: String) = s.substringAfter('T', "").take(5).ifEmpty { null }
    val a = t(startIso)
    val b = t(endIso)
    return if (a != null && b != null) "$a – $b" else "Timed"
  }
}

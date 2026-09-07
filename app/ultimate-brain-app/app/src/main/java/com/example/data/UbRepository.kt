package com.example.data

import com.example.BuildConfig
import com.example.data.notion.NotionClient
import com.example.data.notion.NotionConfig
import com.example.data.notion.NotionMappers
import com.example.data.notion.NotionPage
import com.example.data.notion.plainTitle
import com.example.data.notion.prop
import com.example.model.GoalModel
import com.example.model.NoteModel
import com.example.model.Priority
import com.example.model.ProjectModel
import com.example.model.TagModel
import com.example.model.Task
import com.example.model.TaskStatus
import com.example.model.WorkSessionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class LibraryData(
  val people: List<com.example.model.PersonModel> = emptyList(),
  val books: List<com.example.model.BookModel> = emptyList(),
  val readingLog: List<com.example.model.ReadingLogModel> = emptyList(),
  val genres: List<com.example.model.GenreModel> = emptyList(),
  val recipes: List<com.example.model.RecipeModel> = emptyList(),
  val mealPlan: List<com.example.model.MealPlanModel> = emptyList(),
  val workSessions: List<com.example.model.WorkSessionModel> = emptyList(),
)

/** Everything the app pulls from Notion in one shot. */
data class WorkspaceData(
  val tasks: List<Task>,
  val projects: List<ProjectModel>,
  val notes: List<NoteModel>,
  val goals: List<GoalModel>,
  val tags: List<TagModel>,
  val milestones: List<com.example.model.MilestoneModel> = emptyList(),
)

/**
 * Single entry point for Notion Direct-API reads and writes. Falls back to
 * [DummyData] when [NotionConfig.isConfigured] is false.
 */
class UbRepository(
  private val client: NotionClient? =
    if (NotionConfig.isConfigured) NotionClient(NotionConfig.integrationToken, enableLogging = BuildConfig.DEBUG)
    else null,
) {

  val isRemote: Boolean get() = client != null

  suspend fun loadWorkspace(doneLookbackDays: Long = 60): WorkspaceData = withContext(Dispatchers.IO) {
    val c = client ?: return@withContext dummyWorkspace()

    // Server-side filters keep the payload sane — the Tasks DB alone has 600+
    // rows of history. We pull open work plus anything closed in the last week.
    // Everything any screen can surface: in-progress, planned for today,
    // scheduled (has a due date), or closed in the last week. Excludes the
    // undated backlog, which the app never shows.
    val dueCutoff = java.time.LocalDate.now().minusDays(45).toString()
    val doneCutoff = java.time.LocalDate.now().minusDays(doneLookbackDays).toString()
    val openOrRecentTasks = mapOf<String, Any>(
      "or" to listOf(
        mapOf("property" to "Status", "status" to mapOf("does_not_equal" to "Done")),
        mapOf("property" to "My Day", "checkbox" to mapOf("equals" to true)),
        mapOf("property" to "Due", "date" to mapOf("on_or_after" to dueCutoff)),
        mapOf("property" to "Completed", "date" to mapOf("on_or_after" to doneCutoff)),
      ),
    )
    val notArchivedProjects = mapOf<String, Any>(
      "property" to "Archived", "checkbox" to mapOf("equals" to false),
    )
    val notesByDate = listOf(mapOf<String, Any>("property" to "Note Date", "direction" to "descending"))
    // Due ascending → overdue, then today, then upcoming land in the earliest
    // pages, so the near-term work is always complete even if the page cap bites.
    val tasksByDue = listOf(mapOf<String, Any>("property" to "Due", "direction" to "ascending"))

    // A filter referencing a property the workspace doesn't have would 400 and
    // sink the whole load, so each falls back to an unfiltered query.
    suspend fun q(dsId: String, filter: Map<String, Any>? = null, sorts: List<Map<String, Any>>? = null, maxPages: Int = 6) =
      try {
        c.queryAll(dsId, filter, sorts, maxPages)
      } catch (e: retrofit2.HttpException) {
        if (e.code() == 400 && (filter != null || sorts != null)) c.queryAll(dsId, maxPages = maxPages) else throw e
      }

    val (taskPages, projectPages, notePages, goalPages, tagPages, milestonePages) = coroutineScope {
      // The chips (Today / This week / Overdue / Active projects / Inbox /
      // Recurring) collectively span the whole open task set, so we have to
      // load all of it. queryAll stops as soon as has_more is false — the cap
      // is just a safety ceiling for pathological workspaces.
      val t = async {
        q(
          NotionConfig.tasksDsId,
          filter = openOrRecentTasks,
          sorts = tasksByDue,
          maxPages = if (doneLookbackDays > 90) 40 else 25,
        )
      }
      val p = async { q(NotionConfig.projectsDsId, filter = notArchivedProjects) }
      val n = async { q(NotionConfig.notesDsId, sorts = notesByDate, maxPages = 2) }
      val g = async { q(NotionConfig.goalsDsId) }
      val tag = async { if (NotionConfig.tagsDsId.isNotBlank()) q(NotionConfig.tagsDsId) else emptyList() }
      val ms = async { if (NotionConfig.milestonesDsId.isNotBlank()) q(NotionConfig.milestonesDsId, maxPages = 2) else emptyList() }
      SixLists(t.await(), p.await(), n.await(), g.await(), tag.await(), ms.await())
    }

    val alive = { page: NotionPage -> !page.archived && !page.inTrash }
    val tagNames = tagPages.filter(alive).associate { it.id to it.properties.prop("Name")?.plainTitle().orEmpty() }
    val projectNames = projectPages.filter(alive).associate { it.id to it.properties.prop("Name")?.plainTitle().orEmpty() }
    val goalNames = goalPages.filter(alive).associate { it.id to it.properties.prop("Name")?.plainTitle().orEmpty() }

    val tasks = taskPages.filter(alive).map { NotionMappers.toTask(it, projectNames, tagNames) }
    val tasksByProject = tasks.filter { it.projectId != null }.groupBy { it.projectId!! }

    val projects = projectPages.filter(alive).map { NotionMappers.toProject(it, goalNames, tagNames, tasksByProject) }
    val projectsById = projects.associateBy { it.id }

    val notes = notePages.filter(alive).map { NotionMappers.toNote(it, projectNames, tagNames) }
    val goals = goalPages.filter(alive).map { NotionMappers.toGoal(it, projectsById, tagNames) }
    val tags = tagPages.filter(alive).map { NotionMappers.toTag(it) }
      .map { it.copy(parentName = it.parentId?.let { pid -> tagNames[pid] }) }
    val milestones = milestonePages.filter(alive).map { NotionMappers.toMilestone(it, goalNames) }

    WorkspaceData(tasks, projects, notes, goals, tags, milestones)
  }

  suspend fun loadLibrary(): LibraryData = withContext(Dispatchers.IO) {
    val c = client ?: return@withContext LibraryData()
    suspend fun q(id: String, max: Int = 4) =
      if (id.isBlank()) emptyList() else try { c.queryAll(id, maxPages = max) } catch (_: Exception) { emptyList() }
    val (peoplePages, bookPages, logPages, genrePages, recipePages, mealPages) = coroutineScope {
      val a = async { q(NotionConfig.peopleDsId) }
      val b = async { q(NotionConfig.booksDsId) }
      val d = async { q(NotionConfig.readingLogDsId, 3) }
      val e = async { q(NotionConfig.genresDsId) }
      val f = async { q(NotionConfig.recipesDsId) }
      val g = async { q(NotionConfig.mealPlannerDsId, 3) }
      SixLists(a.await(), b.await(), d.await(), e.await(), f.await(), g.await())
    }
    val alive = { p: NotionPage -> !p.archived && !p.inTrash }
    val bookTitles = bookPages.filter(alive).associate { it.id to (it.properties.prop("Title", "Name")?.plainTitle().orEmpty()) }
    val recipeNames = recipePages.filter(alive).associate { it.id to (it.properties.prop("Name")?.plainTitle().orEmpty()) }
    val sessionPages = if (NotionConfig.workSessionsDsId.isBlank()) emptyList() else try {
      c.queryAll(
        NotionConfig.workSessionsDsId,
        sorts = listOf(mapOf("property" to "Start", "direction" to "descending")),
        maxPages = 2,
      )
    } catch (_: Exception) { emptyList() }
    LibraryData(
      people = peoplePages.filter(alive).map { NotionMappers.toPerson(it) },
      books = bookPages.filter(alive).map { NotionMappers.toBook(it) },
      readingLog = logPages.filter(alive).map { NotionMappers.toReadingLog(it, bookTitles) },
      genres = genrePages.filter(alive).map { NotionMappers.toGenre(it) },
      recipes = recipePages.filter(alive).map { NotionMappers.toRecipe(it) },
      mealPlan = mealPages.filter(alive).map { NotionMappers.toMealPlan(it, recipeNames) },
      workSessions = sessionPages.filter(alive)
        .map { NotionMappers.toWorkSession(it, emptyMap()) }
        .sortedByDescending { it.startIso ?: "" },
    )
  }

  /** Set a select/status/checkbox/date on any page — used by the library editors. */
  suspend fun setPageStatus(pageId: String, prop: String, name: String?) {
    val c = client ?: return
    val v: Any = mapOf("status" to (name?.let { mapOf("name" to it) }))
    c.call { it.updatePage(pageId, mapOf("properties" to mapOf(prop to v))) }
  }
  suspend fun setPageSelect(pageId: String, prop: String, name: String?) {
    val c = client ?: return
    val v: Any = mapOf("select" to (name?.let { mapOf("name" to it) }))
    c.call { it.updatePage(pageId, mapOf("properties" to mapOf(prop to v))) }
  }
  suspend fun setPageCheckbox(pageId: String, prop: String, value: Boolean) {
    val c = client ?: return
    c.call { it.updatePage(pageId, mapOf("properties" to mapOf(prop to mapOf("checkbox" to value)))) }
  }
  suspend fun createInDb(dsId: String, titleProp: String, title: String, extra: Map<String, Any> = emptyMap()): String? {
    val c = client ?: return null
    if (dsId.isBlank()) return null
    val props = buildMap<String, Any> {
      put(titleProp, mapOf("title" to listOf(mapOf("text" to mapOf("content" to title)))))
      putAll(extra)
    }
    return c.call { it.createPage(mapOf("parent" to mapOf("data_source_id" to dsId), "properties" to props)) }.id
  }

  suspend fun setMilestoneGoal(milestoneId: String, goalId: String?) {
    val c = client ?: return
    val rel = if (goalId == null) emptyList<Any>() else listOf(mapOf("id" to goalId))
    c.call { it.updatePage(milestoneId, mapOf("properties" to mapOf("Goal" to mapOf("relation" to rel)))) }
  }
  suspend fun setMilestoneDate(milestoneId: String, iso: String?) {
    val c = client ?: return
    val v: Any = mapOf("date" to (iso?.let { mapOf("start" to it) }))
    c.call { it.updatePage(milestoneId, mapOf("properties" to mapOf("Target Deadline" to v))) }
  }
  suspend fun setMilestoneCompleted(milestoneId: String, completed: Boolean) {
    val c = client ?: return
    val iso = if (completed) java.time.LocalDate.now().toString() else null
    val value: Any = mapOf("date" to (iso?.let { mapOf("start" to it) }))
    c.call { it.updatePage(milestoneId, mapOf("properties" to mapOf("Date Completed" to value))) }
  }

  // --- writes ---------------------------------------------------------------

  suspend fun setTaskStatus(taskId: String, status: TaskStatus) {
    val c = client ?: return
    val name = when (status) {
      TaskStatus.TODO -> "To Do"
      TaskStatus.DOING -> "Doing"
      TaskStatus.DONE -> "Done"
    }
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf("Status" to mapOf("status" to mapOf("name" to name))))) }
  }

  suspend fun setTaskMyDay(taskId: String, myDay: Boolean) {
    val c = client ?: return
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf("My Day" to mapOf("checkbox" to myDay)))) }
  }

  suspend fun setNoteFavorite(noteId: String, favorite: Boolean) {
    val c = client ?: return
    c.call { it.updatePage(noteId, mapOf("properties" to mapOf("Favorite" to mapOf("checkbox" to favorite)))) }
  }

  suspend fun updateNoteMeta(noteId: String, title: String, type: String) {
    val c = client ?: return
    c.call {
      it.updatePage(
        noteId,
        mapOf("properties" to mapOf(
          "Name" to mapOf("title" to listOf(mapOf("text" to mapOf("content" to title)))),
          "Type" to mapOf("select" to mapOf("name" to type)),
        )),
      )
    }
  }

  suspend fun setProjectArchived(projectId: String, archived: Boolean) {
    val c = client ?: return
    c.call { it.updatePage(projectId, mapOf("properties" to mapOf("Archived" to mapOf("checkbox" to archived)))) }
  }

  suspend fun setTaskPriority(taskId: String, priority: com.example.model.Priority?) {
    val c = client ?: return
    val name = when (priority) {
      com.example.model.Priority.HIGH -> "High"
      com.example.model.Priority.MEDIUM -> "Medium"
      com.example.model.Priority.LOW -> "Low"
      null -> null
    }
    val value: Any = if (name == null) mapOf("status" to null) else mapOf("status" to mapOf("name" to name))
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf("Priority" to value))) }
  }

  suspend fun updateProject(projectId: String, name: String, status: String, deadlineIso: String?) {
    val c = client ?: return
    val props = buildMap<String, Any> {
      put("Name", mapOf("title" to listOf(mapOf("text" to mapOf("content" to name)))))
      put("Status", mapOf("status" to mapOf("name" to status)))
      if (deadlineIso != null) put("Target Deadline", mapOf("date" to mapOf("start" to deadlineIso)))
    }
    c.call { it.updatePage(projectId, mapOf("properties" to props)) }
  }

  suspend fun setProjectStatus(id: String, status: String) = setPageStatus(id, "Status", status)
  suspend fun setProjectDeadline(id: String, iso: String?) {
    val c = client ?: return
    val v: Any = mapOf("date" to (iso?.let { mapOf("start" to it) }))
    c.call { it.updatePage(id, mapOf("properties" to mapOf("Target Deadline" to v))) }
  }
  suspend fun setProjectRelation(id: String, prop: String, ids: List<String>) {
    val c = client ?: return
    c.call { it.updatePage(id, mapOf("properties" to mapOf(prop to mapOf("relation" to ids.map { i -> mapOf("id" to i) })))) }
  }
  suspend fun setProjectReviewNotes(id: String, text: String) {
    val c = client ?: return
    c.call {
      it.updatePage(id, mapOf("properties" to mapOf(
        "Review Notes" to mapOf("rich_text" to listOf(mapOf("text" to mapOf("content" to text)))),
      )))
    }
  }
  suspend fun setProjectGoal(id: String, goalId: String?) {
    val c = client ?: return
    val rel = if (goalId == null) emptyList<Any>() else listOf(mapOf("id" to goalId))
    c.call { it.updatePage(id, mapOf("properties" to mapOf("Goal" to mapOf("relation" to rel)))) }
  }
  suspend fun setGoalDeadline(id: String, iso: String?) {
    val c = client ?: return
    val v: Any = mapOf("date" to (iso?.let { mapOf("start" to it) }))
    c.call { it.updatePage(id, mapOf("properties" to mapOf("Target Deadline" to v))) }
  }
  suspend fun setGoalDate(id: String, prop: String, iso: String?) {
    val c = client ?: return
    val v: Any = mapOf("date" to (iso?.let { mapOf("start" to it) }))
    c.call { it.updatePage(id, mapOf("properties" to mapOf(prop to v))) }
  }
  suspend fun setTagParent(id: String, parentId: String?) {
    val c = client ?: return
    val rel = if (parentId == null) emptyList<Any>() else listOf(mapOf("id" to parentId))
    c.call { it.updatePage(id, mapOf("properties" to mapOf("Parent Tag" to mapOf("relation" to rel)))) }
  }
  suspend fun setGoalTag(id: String, tagId: String?) {
    val c = client ?: return
    val rel = if (tagId == null) emptyList<Any>() else listOf(mapOf("id" to tagId))
    c.call { it.updatePage(id, mapOf("properties" to mapOf("Tag" to mapOf("relation" to rel)))) }
  }
  suspend fun setNoteDate(id: String, iso: String?) {
    val c = client ?: return
    val v: Any = mapOf("date" to (iso?.let { mapOf("start" to it) }))
    c.call { it.updatePage(id, mapOf("properties" to mapOf("Note Date" to v))) }
  }
  suspend fun setNoteUrl(id: String, url: String) {
    val c = client ?: return
    c.call { it.updatePage(id, mapOf("properties" to mapOf("URL" to mapOf("url" to url.ifBlank { null })))) }
  }
  suspend fun setNoteReviewDate(id: String, iso: String?) {
    val c = client ?: return
    val v: Any = mapOf("date" to (iso?.let { mapOf("start" to it) }))
    c.call { it.updatePage(id, mapOf("properties" to mapOf("Review Date" to v))) }
  }
  suspend fun setNoteRelation(id: String, prop: String, ids: List<String>) {
    val c = client ?: return
    c.call { it.updatePage(id, mapOf("properties" to mapOf(prop to mapOf("relation" to ids.map { i -> mapOf("id" to i) })))) }
  }
  suspend fun setNoteProject(id: String, projectId: String?) {
    val c = client ?: return
    val rel = if (projectId == null) emptyList<Any>() else listOf(mapOf("id" to projectId))
    c.call { it.updatePage(id, mapOf("properties" to mapOf("Project" to mapOf("relation" to rel)))) }
  }

  suspend fun createProject(name: String): String? {
    val c = client ?: return null
    val props = mapOf<String, Any>(
      "Name" to mapOf("title" to listOf(mapOf("text" to mapOf("content" to name)))),
      "Status" to mapOf("status" to mapOf("name" to "Planned")),
    )
    return c.call {
      it.createPage(mapOf("parent" to mapOf("data_source_id" to NotionConfig.projectsDsId), "properties" to props))
    }.id
  }

  suspend fun createNote(title: String, type: String, projectId: String?): String? {
    val c = client ?: return null
    val props = buildMap<String, Any> {
      put("Name", mapOf("title" to listOf(mapOf("text" to mapOf("content" to title)))))
      put("Type", mapOf("select" to mapOf("name" to type)))
      projectId?.let { put("Project", mapOf("relation" to listOf(mapOf("id" to it)))) }
    }
    return c.call {
      it.createPage(mapOf("parent" to mapOf("data_source_id" to NotionConfig.notesDsId), "properties" to props))
    }.id
  }

  /**
   * Live select/status/multi_select option lists, keyed "<db>.<Property>"
   * (e.g. "task.Priority" -> ["Low","Medium","High"]). Fetched once on sync
   * so the editors never rely on a stale hardcoded list.
   */
  suspend fun loadSchemaOptions(): Map<String, List<String>> = withContext(Dispatchers.IO) {
    val c = client ?: return@withContext emptyMap()
    val dbs = listOf(
      "task" to NotionConfig.tasksDsId,
      "project" to NotionConfig.projectsDsId,
      "note" to NotionConfig.notesDsId,
      "goal" to NotionConfig.goalsDsId,
      "tag" to NotionConfig.tagsDsId,
    ).filter { it.second.isNotBlank() }
    val out = LinkedHashMap<String, List<String>>()
    coroutineScope {
      dbs.map { (prefix, id) ->
        async {
          try {
            val schema = c.call { it.getDataSource(id) }
            schema.properties.forEach { (name, prop) ->
              val opts = prop.optionNames()
              if (opts.isNotEmpty()) synchronized(out) { out["$prefix.$name"] = opts }
            }
          } catch (_: Exception) { }
        }
      }.forEach { it.await() }
    }
    out
  }

  /** Workspace members (id -> display name) for people-property pickers. */
  suspend fun loadUsers(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
    val c = client ?: return@withContext emptyList()
    val out = mutableListOf<Pair<String, String>>()
    var cursor: String? = null
    try {
      do {
        val resp = c.call { it.listUsers(cursor, 100) }
        resp.results.filter { it.type == "person" || it.type == null }
          .forEach { u -> if (!u.name.isNullOrBlank()) out.add(u.id to u.name) }
        cursor = if (resp.hasMore) resp.nextCursor else null
      } while (cursor != null)
    } catch (_: Exception) { }
    out
  }

  /** Page body as Markdown, or null (unconfigured, empty, or unsupported). */
  suspend fun getPageBody(pageId: String): String? {
    val c = client ?: return null
    return try {
      c.call { it.getPageMarkdown(pageId) }.markdown?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
      null
    }
  }

  suspend fun setPageBody(pageId: String, markdown: String) {
    val c = client ?: return
    c.call {
      it.replacePageMarkdown(
        pageId,
        mapOf(
          "type" to "replace_content",
          "replace_content" to mapOf("new_str" to markdown, "allow_deleting_content" to true),
        ),
      )
    }
  }

  suspend fun createGoal(name: String): String? {
    val c = client ?: return null
    val props = mapOf<String, Any>(
      "Name" to mapOf("title" to listOf(mapOf("text" to mapOf("content" to name)))),
      "Status" to mapOf("status" to mapOf("name" to "Active")),
    )
    return c.call {
      it.createPage(mapOf("parent" to mapOf("data_source_id" to NotionConfig.goalsDsId), "properties" to props))
    }.id
  }

  suspend fun setGoalStatus(goalId: String, status: String) {
    val c = client ?: return
    c.call { it.updatePage(goalId, mapOf("properties" to mapOf("Status" to mapOf("status" to mapOf("name" to status))))) }
  }

  suspend fun setGoalArchived(goalId: String, archived: Boolean) {
    val c = client ?: return
    c.call { it.updatePage(goalId, mapOf("properties" to mapOf("Archived" to mapOf("checkbox" to archived)))) }
  }

  /** Set a task's Due date (ISO yyyy-MM-dd or full datetime), optionally with an end. */
  suspend fun setTaskDue(taskId: String, iso: String?, endIso: String? = null) {
    val c = client ?: return
    val date: Any? = if (iso == null) null else buildMap<String, Any> {
      put("start", iso); if (endIso != null) put("end", endIso)
    }
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf("Due" to mapOf("date" to date)))) }
  }

  suspend fun setTaskText(taskId: String, prop: String, value: String) {
    val c = client ?: return
    c.call {
      it.updatePage(taskId, mapOf("properties" to mapOf(
        prop to mapOf("rich_text" to listOf(mapOf("text" to mapOf("content" to value)))),
      )))
    }
  }

  /** Set (or clear, value=null) a select property on a task. */
  suspend fun setTaskSelect(taskId: String, prop: String, value: String?) {
    val c = client ?: return
    val v: Any = mapOf("select" to (value?.let { mapOf("name" to it) }))
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf(prop to v))) }
  }

  suspend fun setTaskCheckbox(taskId: String, prop: String, value: Boolean) {
    val c = client ?: return
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf(prop to mapOf("checkbox" to value)))) }
  }

  suspend fun setTaskLabels(taskId: String, labels: List<String>) {
    val c = client ?: return
    c.call {
      it.updatePage(taskId, mapOf("properties" to mapOf(
        "Labels" to mapOf("multi_select" to labels.map { l -> mapOf("name" to l) }),
      )))
    }
  }

  /** Set (or clear, iso=null) a date property on a task. */
  suspend fun setTaskDate(taskId: String, prop: String, iso: String?) {
    val c = client ?: return
    val v: Any = mapOf("date" to (iso?.let { mapOf("start" to it) }))
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf(prop to v))) }
  }

  /** Set a relation property (list of page ids) on a task. */
  suspend fun setTaskRelation(taskId: String, prop: String, ids: List<String>) {
    val c = client ?: return
    c.call {
      it.updatePage(taskId, mapOf("properties" to mapOf(
        prop to mapOf("relation" to ids.map { i -> mapOf("id" to i) }),
      )))
    }
  }

  /** Set a people property (list of workspace-user ids) on a task. */
  suspend fun setTaskPeopleProp(taskId: String, prop: String, userIds: List<String>) {
    val c = client ?: return
    c.call {
      it.updatePage(taskId, mapOf("properties" to mapOf(
        prop to mapOf("people" to userIds.map { i -> mapOf("id" to i) }),
      )))
    }
  }

  /** Set a multi_select property on a task. */
  suspend fun setTaskMulti(taskId: String, prop: String, values: List<String>) {
    val c = client ?: return
    c.call {
      it.updatePage(taskId, mapOf("properties" to mapOf(
        prop to mapOf("multi_select" to values.map { v -> mapOf("name" to v) }),
      )))
    }
  }

  suspend fun setTaskRecurrence(taskId: String, unit: String?, interval: Int) {
    val c = client ?: return
    val props = mapOf<String, Any>(
      "Recur Unit" to mapOf("select" to (unit?.let { mapOf("name" to it) })),
      "Recur Interval" to mapOf("number" to (if (unit == null) null else interval)),
    )
    c.call { it.updatePage(taskId, mapOf("properties" to props)) }
  }

  suspend fun setTaskProject(taskId: String, projectId: String?) {
    val c = client ?: return
    val rel = if (projectId == null) emptyList<Any>() else listOf(mapOf("id" to projectId))
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf("Project" to mapOf("relation" to rel)))) }
  }

  /**
   * Log a completed focus session as a Work Session row (Start / End / Tasks),
   * mirroring the Python server's format_work_session. No-op if the Work
   * Sessions data source isn't configured.
   */
  suspend fun createWorkSession(taskId: String, taskName: String, startIso: String, endIso: String): String? {
    val c = client ?: return null
    if (NotionConfig.workSessionsDsId.isBlank()) return null
    val props = mapOf<String, Any>(
      "Name" to mapOf("title" to listOf(mapOf("text" to mapOf("content" to "Focus · $taskName")))),
      "Start" to mapOf("date" to mapOf("start" to startIso)),
      "End" to mapOf("date" to mapOf("start" to endIso)),
      "Tasks" to mapOf("relation" to listOf(mapOf("id" to taskId))),
    )
    return c.call {
      it.createPage(mapOf("parent" to mapOf("data_source_id" to NotionConfig.workSessionsDsId), "properties" to props))
    }.id
  }

  /** Every Work Session linked to [taskId], newest first. Empty on dummy data / no config. */
  suspend fun loadWorkSessionsForTask(taskId: String): List<WorkSessionModel> = withContext(Dispatchers.IO) {
    val c = client ?: return@withContext emptyList()
    if (NotionConfig.workSessionsDsId.isBlank()) return@withContext emptyList()
    val pages = try {
      c.queryAll(
        NotionConfig.workSessionsDsId,
        filter = mapOf("property" to "Tasks", "relation" to mapOf("contains" to taskId)),
        sorts = listOf(mapOf("property" to "Start", "direction" to "descending")),
        maxPages = 4,
      )
    } catch (_: Exception) {
      emptyList()
    }
    pages.filter { !it.archived && !it.inTrash }.map { NotionMappers.toWorkSession(it, emptyMap()) }
  }

  /** Create a task; returns the new page id, or null when running on dummy data. */
  suspend fun createTask(
    name: String,
    projectId: String?,
    priority: Priority?,
    myDay: Boolean,
    dueIso: String? = null,
    labels: List<String> = emptyList(),
    parentTaskId: String? = null,
  ): String? {
    val c = client ?: return null
    val props = buildMap<String, Any> {
      put("Name", mapOf("title" to listOf(mapOf("text" to mapOf("content" to name)))))
      put("Status", mapOf("status" to mapOf("name" to "To Do")))
      put("My Day", mapOf("checkbox" to myDay))
      priority?.let {
        val pn = when (it) { Priority.HIGH -> "High"; Priority.MEDIUM -> "Medium"; Priority.LOW -> "Low" }
        put("Priority", mapOf("status" to mapOf("name" to pn)))
      }
      projectId?.let { put("Project", mapOf("relation" to listOf(mapOf("id" to it)))) }
      parentTaskId?.let { put("Parent Task", mapOf("relation" to listOf(mapOf("id" to it)))) }
      dueIso?.let { put("Due", mapOf("date" to mapOf("start" to it))) }
      if (labels.isNotEmpty()) put("Labels", mapOf("multi_select" to labels.map { mapOf("name" to it) }))
    }
    val page = c.call {
      it.createPage(mapOf("parent" to mapOf("data_source_id" to NotionConfig.tasksDsId), "properties" to props))
    }
    return page.id
  }

  // --- dummy fallback ------------------------------------------------------

  private fun dummyWorkspace() = WorkspaceData(
    tasks = DummyData.initialTasks,
    projects = DummyData.projectsList,
    notes = DummyData.notesList,
    goals = DummyData.goalsList,
    tags = DummyData.tagsList,
    milestones = DummyData.milestonesList,
  )

  private data class SixLists(
    val a: List<NotionPage>, val b: List<NotionPage>, val c: List<NotionPage>,
    val d: List<NotionPage>, val e: List<NotionPage>, val f: List<NotionPage>,
  )
}

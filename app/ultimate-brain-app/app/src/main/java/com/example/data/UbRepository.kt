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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** Everything the app pulls from Notion in one shot. */
data class WorkspaceData(
  val tasks: List<Task>,
  val projects: List<ProjectModel>,
  val notes: List<NoteModel>,
  val goals: List<GoalModel>,
  val tags: List<TagModel>,
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

  suspend fun loadWorkspace(): WorkspaceData = withContext(Dispatchers.IO) {
    val c = client ?: return@withContext dummyWorkspace()

    // Server-side filters keep the payload sane — the Tasks DB alone has 600+
    // rows of history. We pull open work plus anything closed in the last week.
    // Everything any screen can surface: in-progress, planned for today,
    // scheduled (has a due date), or closed in the last week. Excludes the
    // undated backlog, which the app never shows.
    val dueCutoff = java.time.LocalDate.now().minusDays(30).toString()
    val openOrRecentTasks = mapOf<String, Any>(
      "or" to listOf(
        mapOf("property" to "Status", "status" to mapOf("equals" to "Doing")),
        mapOf("property" to "My Day", "checkbox" to mapOf("equals" to true)),
        mapOf("property" to "Due", "date" to mapOf("on_or_after" to dueCutoff)),
        mapOf("property" to "Completed", "date" to mapOf("past_week" to emptyMap<String, Any>())),
      ),
    )
    val notArchivedProjects = mapOf<String, Any>(
      "property" to "Archived", "checkbox" to mapOf("equals" to false),
    )
    val notesByDate = listOf(mapOf<String, Any>("property" to "Note Date", "direction" to "descending"))

    // A filter referencing a property the workspace doesn't have would 400 and
    // sink the whole load, so each falls back to an unfiltered query.
    suspend fun q(dsId: String, filter: Map<String, Any>? = null, sorts: List<Map<String, Any>>? = null, maxPages: Int = 6) =
      try {
        c.queryAll(dsId, filter, sorts, maxPages)
      } catch (e: retrofit2.HttpException) {
        if (e.code() == 400 && (filter != null || sorts != null)) c.queryAll(dsId, maxPages = maxPages) else throw e
      }

    val (taskPages, projectPages, notePages, goalPages, tagPages) = coroutineScope {
      val t = async { q(NotionConfig.tasksDsId, filter = openOrRecentTasks, maxPages = 4) }
      val p = async { q(NotionConfig.projectsDsId, filter = notArchivedProjects) }
      val n = async { q(NotionConfig.notesDsId, sorts = notesByDate, maxPages = 2) }
      val g = async { q(NotionConfig.goalsDsId) }
      val tag = async { if (NotionConfig.tagsDsId.isNotBlank()) q(NotionConfig.tagsDsId) else emptyList() }
      FiveLists(t.await(), p.await(), n.await(), g.await(), tag.await())
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

    WorkspaceData(tasks, projects, notes, goals, tags)
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

  suspend fun setProjectArchived(projectId: String, archived: Boolean) {
    val c = client ?: return
    c.call { it.updatePage(projectId, mapOf("properties" to mapOf("Archived" to mapOf("checkbox" to archived)))) }
  }

  suspend fun setGoalStatus(goalId: String, status: String) {
    val c = client ?: return
    c.call { it.updatePage(goalId, mapOf("properties" to mapOf("Status" to mapOf("status" to mapOf("name" to status))))) }
  }

  /** Set a task's Due date (ISO yyyy-MM-dd or full datetime). */
  suspend fun setTaskDue(taskId: String, iso: String) {
    val c = client ?: return
    c.call { it.updatePage(taskId, mapOf("properties" to mapOf("Due" to mapOf("date" to mapOf("start" to iso))))) }
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

  /** Create a task; returns the new page id, or null when running on dummy data. */
  suspend fun createTask(
    name: String,
    projectId: String?,
    priority: Priority?,
    myDay: Boolean,
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
  )

  private data class FiveLists(
    val a: List<NotionPage>, val b: List<NotionPage>, val c: List<NotionPage>,
    val d: List<NotionPage>, val e: List<NotionPage>,
  )
}

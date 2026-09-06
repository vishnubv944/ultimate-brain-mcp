package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.FilterOption
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedFilter
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.viewmodel.MyDayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val scope = uiState.globalSearchScope
  val q = uiState.globalSearchQuery.trim()

  fun show(s: String) = scope == "All" || scope == s

  val goals = if (q.isBlank()) emptyList() else uiState.goals.filter { it.name.contains(q, true) }
  val tags = if (q.isBlank()) emptyList() else uiState.tags.filter { it.name.contains(q, true) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        title = {
          OutlinedTextField(
            value = uiState.globalSearchQuery,
            onValueChange = { viewModel.setGlobalSearchQuery(it) },
            placeholder = { Text("Search everything") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
              if (uiState.globalSearchQuery.isNotEmpty()) {
                IconButton(onClick = { viewModel.setGlobalSearchQuery("") }) {
                  Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("global_search_input"),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
            ),
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )
    },
  ) { pad ->
    val filters = listOf(
      FilterOption("All", "All", uiState.totalSearchMatchesCount + goals.size + tags.size),
      FilterOption("Tasks", "Tasks", uiState.globalSearchTasks.size),
      FilterOption("Projects", "Projects", uiState.globalSearchProjects.size),
      FilterOption("Notes", "Notes", uiState.globalSearchNotes.size),
      FilterOption("Goals", "Goals", goals.size),
      FilterOption("Tags", "Tags", tags.size),
    )
    Column(Modifier.fillMaxSize().padding(pad)) {
      SegmentedFilter(options = filters, selected = scope, onSelect = { viewModel.setGlobalSearchScope(it) })
      Spacer(Modifier.height(4.dp))

      if (q.isBlank()) {
        EmptyLine("Type to search tasks, projects, notes, goals and tags.", Modifier.padding(TodayPad))
        return@Column
      }

      LazyColumn(Modifier.fillMaxSize()) {
        section("Tasks", show("Tasks"), uiState.globalSearchTasks) { t ->
          EntityRow(
            title = t.name,
            meta = "${t.projectName ?: "Inbox"}  ·  ${t.dueDisplay}",
            leadingCheck = t.isDone,
            onClick = { viewModel.openTaskDetail(t.id) },
            strikethrough = t.isDone,
            modifier = Modifier.padding(horizontal = TodayPad),
          )
        }
        section("Projects", show("Projects"), uiState.globalSearchProjects) { p ->
          EntityRow(
            title = p.name, meta = "${p.status}  ·  ${p.progressText}",
            leadingIcon = Icons.Default.Folder, leadingIconTint = MaterialTheme.colorScheme.entityProjects,
            onClick = { viewModel.openProjectDetail(p.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
        }
        section("Notes", show("Notes"), uiState.globalSearchNotes) { n ->
          EntityRow(
            title = n.title, meta = n.type,
            leadingIcon = Icons.Default.Description, leadingIconTint = MaterialTheme.colorScheme.entityNotes,
            onClick = { viewModel.openNoteDetail(n.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
        }
        section("Goals", show("Goals"), goals) { g ->
          EntityRow(
            title = g.name, meta = "${g.status}  ·  ${g.aggregatedProgressText}",
            leadingIcon = Icons.Default.Flag, leadingIconTint = MaterialTheme.colorScheme.entityGoals,
            onClick = { viewModel.openGoalDetail(g.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
        }
        section("Tags", show("Tags"), tags) { tag ->
          EntityRow(
            title = tag.name, meta = tag.type,
            leadingIcon = Icons.Default.Tag, leadingIconTint = MaterialTheme.colorScheme.entityTagArea,
            onClick = { viewModel.openTagDetail(tag.id) },
            modifier = Modifier.padding(horizontal = TodayPad),
          )
        }
        item { Spacer(Modifier.height(96.dp)) }
      }
    }
  }
}

private fun <T> androidx.compose.foundation.lazy.LazyListScope.section(
  title: String,
  visible: Boolean,
  rows: List<T>,
  row: @Composable (T) -> Unit,
) {
  if (!visible || rows.isEmpty()) return
  item(key = "h_$title") { SectionHeader(title, rows.size, Modifier.padding(horizontal = TodayPad)) }
  items(rows, key = { "${title}_${it.hashCode()}" }) { row(it) }
}

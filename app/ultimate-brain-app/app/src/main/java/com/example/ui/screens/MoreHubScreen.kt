package com.example.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.EntityRow
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SectionHeader
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.ui.theme.entityGoals
import com.example.ui.theme.entityNotes
import com.example.ui.theme.entityProjects
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.entityTagEntity
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel

private data class HubEntry(
  val icon: ImageVector,
  val tint: Color,
  val title: String,
  val meta: String,
  val screen: AppScreen,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val cs = MaterialTheme.colorScheme

  val productivity = listOf(
    HubEntry(Icons.Default.TrackChanges, cs.entityGoals, "Goals",
      "${uiState.goals.count { !it.isArchived && it.status != "Achieved" }} active · milestones inside", AppScreen.GOALS),
    HubEntry(Icons.Default.Tag, cs.entityTagArea, "Tags & Areas",
      "${uiState.tags.size} tags", AppScreen.TAGS),
    HubEntry(Icons.Default.Timelapse, cs.primary, "Work sessions",
      "Focus timer & history", AppScreen.WORK_SESSIONS),
  )
  val library = listOf(
    HubEntry(Icons.Default.Person, cs.entityTagEntity, "People", "${uiState.people.size}", AppScreen.PEOPLE),
    HubEntry(Icons.Default.MenuBook, cs.entityNotes, "Books", "${uiState.books.size}", AppScreen.BOOKS),
    HubEntry(Icons.Default.Timeline, cs.onSurfaceVariant, "Reading log", "", AppScreen.READING_LOG),
    HubEntry(Icons.Default.Restaurant, cs.entityGoals, "Recipes", "${uiState.recipes.size}", AppScreen.RECIPES),
    HubEntry(Icons.Default.CalendarMonth, cs.primary, "Meal planner", "", AppScreen.MEAL_PLANNER),
    HubEntry(Icons.Default.LocalOffer, cs.onSurfaceVariant, "Genres", "", AppScreen.GENRES),
  )
  val system = listOf(
    HubEntry(Icons.Default.Search, cs.onSurfaceVariant, "Search", "Everything", AppScreen.GLOBAL_SEARCH),
    HubEntry(Icons.Default.Settings, cs.onSurfaceVariant, "Settings",
      if (uiState.isRemote) "Notion connected" else "Sample data", AppScreen.SETTINGS),
  )

  ScreenScaffold(
    title = "More",
    viewModel = viewModel,
    active = BottomNavDestination.MORE,
    modifier = modifier,
  ) { pad ->
    LazyColumn(Modifier.padding(pad)) {
      hubSection(this, "Productivity", productivity, viewModel)
      hubSection(this, "Library", library, viewModel)
      hubSection(this, "System", system, viewModel)
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
}

private fun hubSection(
  scope: androidx.compose.foundation.lazy.LazyListScope,
  title: String,
  entries: List<HubEntry>,
  viewModel: MyDayViewModel,
) {
  scope.item(key = "h_$title") {
    SectionHeader(title, modifier = Modifier.padding(horizontal = TodayPad))
  }
  scope.itemsIndexed(entries, key = { _, e -> "${title}_${e.title}" }) { index, e ->
    if (index > 0) ThinDivider(Modifier.padding(horizontal = TodayPad))
    EntityRow(
      title = e.title,
      onClick = { viewModel.navigateTo(e.screen) },
      meta = e.meta.ifBlank { null },
      leadingIcon = e.icon,
      leadingIconTint = e.tint,
      modifier = Modifier.padding(horizontal = TodayPad),
    )
  }
}

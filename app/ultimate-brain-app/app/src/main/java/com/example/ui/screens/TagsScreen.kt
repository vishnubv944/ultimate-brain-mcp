package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TagModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.theme.entityTagArea
import com.example.ui.theme.entityTagEntity
import com.example.ui.theme.entityTagResource
import com.example.ui.theme.warning
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.TagFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
  viewModel: MyDayViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()

  // Tab options — enum-driven so the chip click handler passes typed values
  // to ViewModel.selectTagFilter(...) instead of raw strings.
  val tabs = listOf(
    TagFilter.ALL to R.string.tags_tab_all,
    TagFilter.AREAS to R.string.tags_tab_areas,
    TagFilter.RESOURCES to R.string.tags_tab_resources,
    TagFilter.ENTITIES to R.string.tags_tab_entities
  )

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = { Text(stringResource(R.string.tags_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.MORE_HUB) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
          }
        },
        actions = {
          IconButton(onClick = { viewModel.navigateTo(AppScreen.GLOBAL_SEARCH) }) {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          IconButton(onClick = { /* more */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
      )
    },
    bottomBar = {
      BottomNavBar(
        activeDestination = BottomNavDestination.MORE,
        onDestinationSelected = { dest ->
          when (dest) {
            BottomNavDestination.TODAY -> viewModel.navigateTo(AppScreen.TODAY)
            BottomNavDestination.TASKS -> viewModel.navigateTo(AppScreen.TASKS)
            BottomNavDestination.PROJECTS -> viewModel.navigateTo(AppScreen.PROJECTS)
            BottomNavDestination.NOTES -> viewModel.navigateTo(AppScreen.NOTES)
            BottomNavDestination.MORE -> viewModel.navigateTo(AppScreen.MORE_HUB)
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { /* add tag */ },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.tags_fab_new), fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("new_tag_fab")
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Filter chips (lives below the MediumTopAppBar in the body so it scrolls
      // with content; topBar slot stays reserved for the title).
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        tabs.forEach { (filter, labelRes) ->
          val isSelected = uiState.selectedTagFilter == filter
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.selectTagFilter(filter) },
            label = { Text(stringResource(labelRes), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        items(uiState.filteredTags, key = { it.id }) { tag ->
          TagItemCard(
            tag = tag,
            onClick = { viewModel.openTagDetail(tag.id) }
        )
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
      }
    }
  }
}

@Composable
fun TagItemCard(
  tag: TagModel,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val iconVector: ImageVector = when (tag.icon) {
    "corporate_fare" -> Icons.Default.CorporateFare
    "favorite" -> Icons.Default.Favorite
    "payments" -> Icons.Default.Payments
    "terminal" -> Icons.Default.Terminal
    "palette" -> Icons.Default.Palette
    "bolt" -> Icons.Default.Bolt
    "person" -> Icons.Default.Person
    else -> Icons.Default.Tag
  }

  val typeColor = when (tag.type) {
    "Area" -> MaterialTheme.colorScheme.entityTagArea
    "Resource" -> MaterialTheme.colorScheme.entityTagResource
    else -> MaterialTheme.colorScheme.entityTagEntity
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("tag_card_${tag.id}")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(typeColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(iconVector, contentDescription = null, tint = typeColor, modifier = Modifier.size(18.dp))
          }

          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(tag.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
              if (tag.isFavorite) {
                Icon(Icons.Default.Star, contentDescription = stringResource(R.string.action_favorite), tint = MaterialTheme.colorScheme.warning, modifier = Modifier.size(16.dp))
              }
            }
            Text(stringResource(R.string.tags_items_count_format, tag.type, tag.totalItems, tag.activeItems), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
          Text(
            text = stringResource(R.string.tags_projects_notes_format, tag.projectsCount, tag.notesCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Children nested hierarchy
      if (tag.children.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          tag.children.forEach { child ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("↳", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(child.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
              }
              Text(stringResource(R.string.tags_children_items_format, child.itemsCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }
}

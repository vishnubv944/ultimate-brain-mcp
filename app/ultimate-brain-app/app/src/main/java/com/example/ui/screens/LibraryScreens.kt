package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.DetailScaffold
import com.example.ui.components.EmptyLine
import com.example.ui.components.EntityRow
import com.example.ui.components.MarkdownBody
import com.example.ui.components.SectionHeader
import com.example.ui.components.Stat
import com.example.ui.components.StatCard
import com.example.ui.components.OptionRow
import com.example.ui.components.ThinDivider
import com.example.ui.components.TodayPad
import com.example.viewmodel.MyDayViewModel

@Composable
private fun CreateDialog(label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) =
  com.example.ui.components.NameDialog(label, onDismiss, onConfirm)

private fun addFab(onClick: () -> Unit): @Composable () -> Unit = {
  FloatingActionButton(
    onClick = onClick,
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
  ) { Icon(Icons.Default.Add, contentDescription = "New") }
}

// ---- People ----------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  var dialog by remember { mutableStateOf(false) }
  DetailScaffold(title = "People", onBack = { viewModel.navigateBack() }, modifier = modifier, fab = addFab { dialog = true }) { pad ->
    LazyColumn(Modifier.padding(pad)) {
      if (uiState.people.isEmpty()) item { EmptyLine("No people yet.", Modifier.padding(horizontal = TodayPad)) }
      itemsIndexed(uiState.people.sortedBy { it.name }, key = { _, p -> p.id }) { i, p ->
        EntityRow(
          title = p.name.ifBlank { "(no name)" },
          meta = listOfNotNull(p.company.ifBlank { null }, p.relationship.firstOrNull(), p.pipelineStatus).joinToString("  ·  "),
          leadingIcon = Icons.Default.Person,
          onClick = { viewModel.openPerson(p.id) },
          modifier = Modifier.padding(horizontal = TodayPad),
        )
        if (i < uiState.people.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
  if (dialog) CreateDialog("person", { dialog = false }) { viewModel.createPerson(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val p = uiState.people.firstOrNull { it.id == uiState.selectedPersonId }
  DetailScaffold(title = "Person", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    if (p == null) { EmptyLine("Not found.", Modifier.padding(pad).padding(TodayPad)); return@DetailScaffold }
    Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = TodayPad)) {
      Spacer(Modifier.height(8.dp))
      Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      if (p.title.isNotBlank() || p.company.isNotBlank())
        Text(listOfNotNull(p.title.ifBlank { null }, p.company.ifBlank { null }).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      SectionHeader("Contact")
      field("Email", p.email); field("Phone", p.phone); field("Location", p.location)
      field("LinkedIn", p.linkedIn); field("Twitter/X", p.twitter); field("Website", p.website)
      SectionHeader("Relationship")
      Text((p.relationship + listOfNotNull(p.pipelineStatus)).joinToString(", ").ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
      field("Birthday", p.birthday ?: ""); field("Last check-in", p.lastCheckIn ?: "")
      Spacer(Modifier.height(96.dp))
    }
  }
}

@Composable
private fun field(label: String, value: String) {
  if (value.isBlank()) return
  Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp).width(96.dp))
    Text(value, style = MaterialTheme.typography.bodyMedium)
  }
}

// ---- Books ----------------------------------------------------------------

private val BOOK_STATUS = listOf("Want to Read", "Reading", "On Hold", "Read")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  var dialog by remember { mutableStateOf(false) }
  DetailScaffold(title = "Books", onBack = { viewModel.navigateBack() }, modifier = modifier, fab = addFab { dialog = true }) { pad ->
    LazyColumn(Modifier.padding(pad)) {
      item {
        Spacer(Modifier.height(12.dp))
        StatCard(listOf(
          Stat(uiState.books.count { it.status == "Reading" }.toString(), "reading"),
          Stat(uiState.books.count { it.status == "Read" }.toString(), "read"),
          Stat(uiState.books.size.toString(), "total"),
        ), Modifier.padding(horizontal = TodayPad))
        Spacer(Modifier.height(8.dp))
      }
      if (uiState.books.isEmpty()) item { EmptyLine("No books yet.", Modifier.padding(horizontal = TodayPad)) }
      itemsIndexed(uiState.books.sortedBy { it.title }, key = { _, b -> b.id }) { i, b ->
        EntityRow(
          title = b.title,
          meta = listOfNotNull(b.author.ifBlank { null }, b.status, b.rating).joinToString("  ·  "),
          leadingIcon = Icons.Default.MenuBook,
          onClick = { viewModel.openBook(b.id) },
          modifier = Modifier.padding(horizontal = TodayPad),
        )
        if (i < uiState.books.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
  if (dialog) CreateDialog("book", { dialog = false }) { viewModel.createBook(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val b = uiState.books.firstOrNull { it.id == uiState.selectedBookId }
  DetailScaffold(title = "Book", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    if (b == null) { EmptyLine("Not found.", Modifier.padding(pad).padding(TodayPad)); return@DetailScaffold }
    Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = TodayPad)) {
      Spacer(Modifier.height(8.dp))
      Text(b.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      if (b.author.isNotBlank()) Text(b.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      SectionHeader("Details")
      OptionRow("Status", b.status, viewModel.uiState.value.optionsFor("book.Status", BOOK_STATUS), { it?.let { s -> viewModel.setBookStatus(b.id, s) } }, allowClear = false)
      field("Rating", b.rating ?: "")
      field("Pages", b.pages?.toString() ?: "")
      field("Published", b.publishYear?.toString() ?: "")
      field("Started", b.dateStarted ?: ""); field("Finished", b.dateFinished ?: "")
      if (b.ownedFormats.isNotEmpty()) field("Formats", b.ownedFormats.joinToString(", "))
      if (b.shelf.isNotEmpty()) field("Shelf", b.shelf.joinToString(", "))
      if (b.description.isNotBlank()) { SectionHeader("Description"); Text(b.description, style = MaterialTheme.typography.bodyMedium) }
      if (!uiState.detailBody.isNullOrBlank() && uiState.detailBodyForId == b.id &&
        com.example.ui.components.markdownHasRenderableContent(uiState.detailBody!!)
      ) { SectionHeader("Notes"); MarkdownBody(uiState.detailBody!!) }
      Spacer(Modifier.height(96.dp))
    }
  }
}

// ---- Recipes ------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  var dialog by remember { mutableStateOf(false) }
  DetailScaffold(title = "Recipes", onBack = { viewModel.navigateBack() }, modifier = modifier, fab = addFab { dialog = true }) { pad ->
    LazyColumn(Modifier.padding(pad)) {
      if (uiState.recipes.isEmpty()) item { EmptyLine("No recipes yet.", Modifier.padding(horizontal = TodayPad)) }
      itemsIndexed(uiState.recipes.sortedBy { it.name }, key = { _, r -> r.id }) { i, r ->
        val total = (r.prepTime ?: 0) + (r.cookTime ?: 0)
        EntityRow(
          title = r.name,
          meta = listOfNotNull(r.mealTimes.firstOrNull(), if (total > 0) "$total min" else null, if (r.servings != null) "serves ${r.servings}" else null).joinToString("  ·  "),
          leadingIcon = Icons.Default.Restaurant,
          onClick = { viewModel.openRecipe(r.id) },
          modifier = Modifier.padding(horizontal = TodayPad),
        )
        if (i < uiState.recipes.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
  if (dialog) CreateDialog("recipe", { dialog = false }) { viewModel.createRecipe(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  val r = uiState.recipes.firstOrNull { it.id == uiState.selectedRecipeId }
  DetailScaffold(title = "Recipe", onBack = { viewModel.navigateBack() }, modifier = modifier) { pad ->
    if (r == null) { EmptyLine("Not found.", Modifier.padding(pad).padding(TodayPad)); return@DetailScaffold }
    Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = TodayPad)) {
      Spacer(Modifier.height(8.dp))
      Text(r.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      SectionHeader("Details")
      field("Chef", r.chef)
      field("Prep", r.prepTime?.let { "$it min" } ?: "")
      field("Cook", r.cookTime?.let { "$it min" } ?: "")
      field("Servings", r.servings?.toString() ?: "")
      if (r.mealTimes.isNotEmpty()) field("Meal", r.mealTimes.joinToString(", "))
      field("URL", r.url)
      TextButton(onClick = { viewModel.toggleRecipeFavorite(r.id) }) { Text(if (r.favorite) "★ Favorited" else "☆ Add to favorites") }
      if (!uiState.detailBody.isNullOrBlank() && uiState.detailBodyForId == r.id &&
        com.example.ui.components.markdownHasRenderableContent(uiState.detailBody!!)
      ) { SectionHeader("Recipe"); MarkdownBody(uiState.detailBody!!) }
      Spacer(Modifier.height(96.dp))
    }
  }
}

// ---- Reading Log / Genres / Meal Planner (list only) -------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingLogScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  var dialog by remember { mutableStateOf(false) }
  DetailScaffold(title = "Reading log", onBack = { viewModel.navigateBack() }, modifier = modifier, fab = addFab { dialog = true }) { pad ->
    LazyColumn(Modifier.padding(pad)) {
      if (uiState.readingLog.isEmpty()) item { EmptyLine("No log entries yet.", Modifier.padding(horizontal = TodayPad)) }
      itemsIndexed(uiState.readingLog.sortedByDescending { it.logDate }, key = { _, l -> l.id }) { i, l ->
        val pages = if (l.startPage != null && l.endPage != null) "p.${l.startPage}–${l.endPage}" else null
        EntityRow(
          title = l.bookTitle ?: l.name.ifBlank { "Entry" },
          meta = listOfNotNull(l.logDate, pages).joinToString("  ·  "),
          leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
          onClick = {},
          modifier = Modifier.padding(horizontal = TodayPad),
        )
        if (i < uiState.readingLog.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
  if (dialog) CreateDialog("log entry", { dialog = false }) { viewModel.createReadingLog(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  var dialog by remember { mutableStateOf(false) }
  DetailScaffold(title = "Genres", onBack = { viewModel.navigateBack() }, modifier = modifier, fab = addFab { dialog = true }) { pad ->
    LazyColumn(Modifier.padding(pad)) {
      if (uiState.genres.isEmpty()) item { EmptyLine("No genres yet.", Modifier.padding(horizontal = TodayPad)) }
      itemsIndexed(uiState.genres.sortedBy { it.name }, key = { _, g -> g.id }) { i, g ->
        EntityRow(title = g.name, meta = if (g.bookCount > 0) "${g.bookCount} books" else null, onClick = {}, leadingDot = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = TodayPad))
        if (i < uiState.genres.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
  if (dialog) CreateDialog("genre", { dialog = false }) { viewModel.createGenre(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(viewModel: MyDayViewModel, modifier: Modifier = Modifier) {
  val uiState by viewModel.uiState.collectAsState()
  var dialog by remember { mutableStateOf(false) }
  DetailScaffold(title = "Meal planner", onBack = { viewModel.navigateBack() }, modifier = modifier, fab = addFab { dialog = true }) { pad ->
    LazyColumn(Modifier.padding(pad)) {
      if (uiState.mealPlan.isEmpty()) item { EmptyLine("Nothing planned.", Modifier.padding(horizontal = TodayPad)) }
      itemsIndexed(uiState.mealPlan.sortedByDescending { it.date }, key = { _, m -> m.id }) { i, m ->
        EntityRow(
          title = m.recipeNames.firstOrNull() ?: m.name.ifBlank { "Meal" },
          meta = listOfNotNull(m.meal, m.date).joinToString("  ·  "),
          leadingIcon = Icons.Default.Restaurant,
          onClick = {},
          modifier = Modifier.padding(horizontal = TodayPad),
        )
        if (i < uiState.mealPlan.lastIndex) ThinDivider(Modifier.padding(horizontal = TodayPad))
      }
      item { Spacer(Modifier.height(96.dp)) }
    }
  }
  if (dialog) CreateDialog("meal", { dialog = false }) { viewModel.createMealPlan(it) }
}

package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.EmptyLine
import com.example.ui.components.MarkdownBody
import com.example.ui.components.RenameDialog
import com.example.ui.components.TodayPad
import com.example.data.hermes.DocumentAttachment
import com.example.data.hermes.ImageAttachment
import com.example.data.hermes.encodeImageForUpload
import com.example.data.hermes.readDocumentForUpload
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatTurn
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.MyDayViewModel
import com.example.viewmodel.PaletteCommand
import com.example.viewmodel.PaletteEntry
import com.example.viewmodel.ToolCall
import com.example.viewmodel.ToolStatus
import com.example.viewmodel.paletteEntries
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
  viewModel: MyDayViewModel,
  chatViewModel: ChatViewModel,
  modifier: Modifier = Modifier,
) {
  val s by chatViewModel.state.collectAsState()
  LaunchedEffect(Unit) { chatViewModel.onResume() }

  val drawer = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var renaming by remember { mutableStateOf<Pair<String, String>?>(null) }

  // Palette commands (/resume, /title) act on drawer/dialog state that lives
  // here, not in the ViewModel — bridge via the one-shot request counters.
  LaunchedEffect(s.requestOpenHistory) { if (s.requestOpenHistory > 0) drawer.open() }
  LaunchedEffect(s.requestRename) {
    if (s.requestRename > 0 && s.activeSessionId != null) renaming = s.activeSessionId!! to (s.activeTitle ?: "")
  }

  ModalNavigationDrawer(
    drawerState = drawer,
    drawerContent = {
      ModalDrawerSheet {
        Row(
          Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Chats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
          TextButton(onClick = { chatViewModel.newChat(); scope.launch { drawer.close() } }) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.size(4.dp)); Text("New")
          }
        }
        HorizontalDivider()
        LazyColumn {
          items(s.sessions, key = { it.id }) { sess ->
            val active = sess.id == s.activeSessionId
            Row(
              Modifier.fillMaxWidth()
                .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .clickable {
                  chatViewModel.openSession(sess.id, sess.title)
                  scope.launch { drawer.close() }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(Modifier.weight(1f)) {
                Text(
                  sess.title ?: sess.preview?.take(40) ?: "Untitled chat",
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                )
                sess.model?.let {
                  Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              IconButton(onClick = { renaming = sess.id to (sess.title ?: "") }) {
                Icon(Icons.Default.Edit, "Rename", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              IconButton(onClick = { chatViewModel.delete(sess.id) }) {
                Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    },
  ) {
    // A dedicated scaffold, not the shared tab ScreenScaffold: the bottom nav
    // must disappear (not just get covered) once the IME opens.
    //
    // While the bottom nav is visible it's the thing that paints over the
    // navigationBars inset (M3's NavigationBar does that internally); once
    // it's hidden for typing, nothing was painting that sliver any more, so
    // the raw (black) window background showed through right above the
    // keyboard. Fix: pad by the union of navigationBars + ime (not ime
    // alone) so the reserved space always covers whichever the system is
    // actually showing there, and paint the theme's background behind the
    // whole screen so any inset gap resolves to the app's color, never
    // black. contentWindowInsets is zeroed so Scaffold doesn't also reserve
    // this space a second time inside the content padding.
    val imeVisible = WindowInsets.isImeVisible
    val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
    Scaffold(
      modifier = modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(bottomInsets),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        Column {
          TopAppBar(
            title = {
              Text(
                s.activeTitle?.substringBefore(" · ")?.take(28) ?: "Hermes",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              )
            },
            actions = {
              IconButton(onClick = { scope.launch { drawer.open() } }) {
                Icon(Icons.Default.History, contentDescription = "Chat history")
              }
              IconButton(onClick = { chatViewModel.newChat() }) {
                Icon(Icons.Default.Add, contentDescription = "New chat")
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
      },
      bottomBar = {
        if (!imeVisible) {
          com.example.ui.components.BottomNavBar(
            activeDestination = com.example.ui.components.BottomNavDestination.CHAT,
            onDestinationSelected = com.example.ui.components.bottomNavHandler(viewModel),
          )
        }
      },
      containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
      Column(Modifier.fillMaxSize().padding(pad)) {
        if (!s.configured) {
          ConfigureHint(viewModel)
          return@Column
        }
        s.error?.let {
          Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer)
              .clickable { chatViewModel.clearError() }.padding(horizontal = TodayPad, vertical = 6.dp),
          ) {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
          }
        }

        val listState = rememberLazyListState()
        LaunchedEffect(s.turns.size, s.turns.lastOrNull()?.text) {
          if (s.turns.isNotEmpty()) listState.animateScrollToItem(s.turns.size - 1)
        }

        Box(Modifier.weight(1f)) {
          when {
            s.historyLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            s.turns.isEmpty() -> EmptyState { chatViewModel.setInput(it); chatViewModel.send() }
            else -> LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(TodayPad),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(s.turns) { turn -> TurnBubble(turn) }
            }
          }
        }

        s.pendingApproval?.let { (_, summary) ->
          Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer)
              .padding(horizontal = TodayPad, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Approve: $summary", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { chatViewModel.resolveApproval(false) }) { Text("Deny") }
            TextButton(onClick = { chatViewModel.resolveApproval(true) }) { Text("Approve") }
          }
        }

        val showPalette = s.input.startsWith("/") && !s.input.contains(" ")
        if (showPalette) {
          CommandPalette(
            query = s.input,
            skills = s.skills,
            onSelect = { chatViewModel.onPaletteSelect(it) },
          )
        }

        Composer(
          value = s.input,
          streaming = s.streaming,
          attachedImage = s.attachedImage,
          attachedDocName = s.attachedDocName,
          onValue = chatViewModel::setInput,
          onSend = chatViewModel::send,
          onStop = chatViewModel::stop,
          onAttachImage = { uri -> chatViewModel.setAttachedImage(uri) },
          onClearAttachImage = { chatViewModel.setAttachedImage(null) },
          onImageEncoded = { uri, attachment -> chatViewModel.setAttachedImage(uri, attachment) },
          onClearAttachDoc = { chatViewModel.setAttachedDoc(null) },
          onDocRead = { doc -> if (doc != null) chatViewModel.setAttachedDoc(doc) else chatViewModel.docAttachFailed() },
        )
      }
    }
  }

  renaming?.let { (id, cur) ->
    RenameDialog("chat", cur, { renaming = null }) { chatViewModel.rename(id, it); renaming = null }
  }
}

@Composable
private fun CommandPalette(
  query: String,
  skills: List<com.example.data.hermes.HermesSkill>,
  onSelect: (PaletteEntry) -> Unit,
) {
  val entries = remember(query, skills) { paletteEntries(query, skills) }
  androidx.compose.material3.Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 3.dp,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).heightIn(max = 280.dp),
  ) {
    if (entries.isEmpty()) {
      Text(
        "No match for \"$query\"",
        Modifier.padding(14.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      LazyColumn {
        val cmds = entries.filterIsInstance<PaletteEntry.Cmd>()
        val skillEntries = entries.filterIsInstance<PaletteEntry.Skill>()
        if (cmds.isNotEmpty()) {
          item { PaletteHeader("Commands") }
          items(cmds) { e ->
            PaletteRow(icon = "⚡", name = e.cmd.label, desc = e.cmd.description) { onSelect(e) }
          }
        }
        if (skillEntries.isNotEmpty()) {
          item { PaletteHeader("Skills · live") }
          items(skillEntries) { e ->
            PaletteRow(icon = "🧠", name = "/" + e.skill.name, desc = e.skill.description ?: "") { onSelect(e) }
          }
        }
      }
    }
  }
}

@Composable
private fun PaletteHeader(label: String) {
  Text(
    label.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
  )
}

@Composable
private fun PaletteRow(icon: String, name: String, desc: String, onClick: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(icon, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.width(10.dp))
    Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.width(8.dp))
    Text(
      desc,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

@Composable
private fun TurnBubble(turn: ChatTurn) {
  val isUser = turn.role == "user"
  Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
    Column(
      Modifier
        .fillMaxWidth(0.85f)
        .then(
          if (isUser) Modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
          else Modifier,
        ),
      horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
      turn.imagePreview?.let { uri ->
        AsyncImage(
          model = uri,
          contentDescription = "Attached image",
          modifier = Modifier
            .heightIn(max = 180.dp)
            .width(180.dp)
            .padding(bottom = if (turn.text.isNotBlank() || turn.docName != null) 6.dp else 0.dp),
        )
      }
      turn.docName?.let { name ->
        AssistChip(
          onClick = {},
          label = { Text(name, maxLines = 1) },
          leadingIcon = { Icon(Icons.Default.InsertDriveFile, null, Modifier.size(AssistChipDefaults.IconSize)) },
          modifier = Modifier.padding(bottom = if (turn.text.isNotBlank()) 6.dp else 0.dp),
        )
      }
      if (turn.tools.isNotEmpty()) {
        ToolChips(turn.tools)
        if (turn.text.isNotBlank()) Spacer(Modifier.height(6.dp))
      }
      if (isUser) {
        if (turn.text.isNotBlank()) Text(turn.text, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyLarge)
      } else if (turn.text.isNotBlank()) {
        MarkdownBody(turn.text)
      } else if (turn.streaming) {
        Text("…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

/** Native Material chips — one per tool call, colored by status. Plain and legible beats a bespoke card. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ToolChips(tools: List<ToolCall>) {
  androidx.compose.foundation.layout.FlowRow(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    tools.forEach { call ->
      val (icon, tint) = when (call.status) {
        ToolStatus.RUNNING -> Icons.Default.Bolt to MaterialTheme.colorScheme.tertiary
        ToolStatus.DONE -> Icons.Default.Check to MaterialTheme.colorScheme.primary
        ToolStatus.FAILED -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
      }
      AssistChip(
        onClick = {},
        label = { Text(call.tool, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = { Icon(icon, null, Modifier.size(AssistChipDefaults.IconSize), tint = tint) },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Composer(
  value: String,
  streaming: Boolean,
  attachedImage: Uri?,
  attachedDocName: String?,
  onValue: (String) -> Unit,
  onSend: () -> Unit,
  onStop: () -> Unit,
  onAttachImage: (Uri?) -> Unit,
  onClearAttachImage: () -> Unit,
  onImageEncoded: (Uri?, ImageAttachment?) -> Unit,
  onClearAttachDoc: () -> Unit,
  onDocRead: (DocumentAttachment?) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var attachMenuOpen by remember { mutableStateOf(false) }

  val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    onAttachImage(uri)
    scope.launch { onImageEncoded(uri, encodeImageForUpload(context, uri)) }
  }
  val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    scope.launch { onDocRead(readDocumentForUpload(context, uri)) }
  }

  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
    attachedImage?.let { uri ->
      AttachmentChip(
        onRemove = onClearAttachImage,
        content = {
          AsyncImage(
            model = uri,
            contentDescription = "Attached image",
            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)),
          )
        },
      )
      Spacer(Modifier.height(6.dp))
    }
    attachedDocName?.let { name ->
      AssistChip(
        onClick = onClearAttachDoc,
        label = { Text(name, maxLines = 1) },
        leadingIcon = { Icon(Icons.Default.InsertDriveFile, null, Modifier.size(AssistChipDefaults.IconSize)) },
        trailingIcon = { Icon(Icons.Default.Close, "Remove", Modifier.size(AssistChipDefaults.IconSize)) },
      )
      Spacer(Modifier.height(6.dp))
    }
    Row(verticalAlignment = Alignment.Bottom) {
      Box {
        IconButton(onClick = { attachMenuOpen = true }) {
          Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = attachMenuOpen, onDismissRequest = { attachMenuOpen = false }) {
          DropdownMenuItem(
            text = { Text("Photo") },
            leadingIcon = { Icon(Icons.Default.Image, null) },
            onClick = {
              attachMenuOpen = false
              imagePicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
          )
          DropdownMenuItem(
            text = { Text("Document") },
            leadingIcon = { Icon(Icons.Default.Description, null) },
            onClick = { attachMenuOpen = false; docPicker.launch("*/*") },
          )
        }
      }
      // Native filled message-field look — no outline, a plain rounded pill
      // on the surface tone, matching stock Android chat inputs.
      TextField(
        value = value,
        onValueChange = onValue,
        placeholder = { Text("Message Hermes… (try \"/\")") },
        modifier = Modifier.weight(1f),
        maxLines = 5,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          disabledIndicatorColor = Color.Transparent,
          focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
      )
      Spacer(Modifier.size(4.dp))
      if (streaming) {
        IconButton(onClick = onStop) {
          Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
        }
      } else {
        IconButton(onClick = onSend, enabled = value.isNotBlank() || attachedImage != null || attachedDocName != null) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }
  }
}

@Composable
private fun AttachmentChip(onRemove: () -> Unit, content: @Composable () -> Unit) {
  Box {
    content()
    IconButton(
      onClick = onRemove,
      modifier = Modifier.size(18.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f), CircleShape),
    ) {
      Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(11.dp))
    }
  }
}

@Composable
private fun EmptyState(onPrompt: (String) -> Unit) {
  val prompts = listOf("Plan my day", "What's overdue?", "Summarise my active projects", "What should I do next?")
  Column(
    Modifier.fillMaxSize().padding(TodayPad),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Ask Hermes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text(
      "Your assistant can read and act on your Ultimate Brain. Type \"/\" for commands and skills.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    prompts.forEach { p ->
      TextButton(onClick = { onPrompt(p) }) { Text(p) }
    }
  }
}

@Composable
private fun ConfigureHint(viewModel: MyDayViewModel) {
  Column(Modifier.fillMaxSize().padding(TodayPad), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Hermes not connected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Text(
      "Add the server URL and key in Settings, then reopen this tab.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) { Text("Open Settings") }
  }
}

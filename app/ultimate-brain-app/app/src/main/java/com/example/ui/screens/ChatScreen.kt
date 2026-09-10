package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.EmptyLine
import com.example.ui.components.MarkdownBody
import com.example.ui.components.RenameDialog
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.TodayPad
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatTurn
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.MyDayViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
                .background(if (active) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
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
                Icon(Icons.Default.History, "Rename", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    ScreenScaffold(
      title = s.activeTitle?.substringBefore(" · ")?.take(28) ?: "Hermes",
      viewModel = viewModel,
      active = BottomNavDestination.CHAT,
      modifier = modifier,
      actions = {
        IconButton(onClick = { scope.launch { drawer.open() } }) {
          Icon(Icons.Default.History, contentDescription = "Chat history")
        }
        IconButton(onClick = { chatViewModel.newChat() }) {
          Icon(Icons.Default.Add, contentDescription = "New chat")
        }
      },
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

        Composer(
          value = s.input,
          streaming = s.streaming,
          onValue = chatViewModel::setInput,
          onSend = chatViewModel::send,
          onStop = chatViewModel::stop,
        )
      }
    }
  }

  renaming?.let { (id, cur) ->
    RenameDialog("chat", cur, { renaming = null }) { chatViewModel.rename(id, it); renaming = null }
  }
}

@Composable
private fun TurnBubble(turn: ChatTurn) {
  val isUser = turn.role == "user"
  Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
    Column(
      Modifier
        .widthIn(max = 320.dp)
        .then(
          if (isUser) Modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
          else Modifier,
        ),
    ) {
      if (turn.tools.isNotEmpty()) {
        turn.tools.takeLast(3).forEach { t ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bolt, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(4.dp))
            Text(t, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        if (turn.text.isNotBlank()) Spacer(Modifier.height(4.dp))
      }
      if (isUser) {
        Text(turn.text, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyMedium)
      } else if (turn.text.isNotBlank()) {
        MarkdownBody(turn.text)
      } else if (turn.streaming) {
        Text("…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Composer(
  value: String,
  streaming: Boolean,
  onValue: (String) -> Unit,
  onSend: () -> Unit,
  onStop: () -> Unit,
) {
  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).imePadding(),
    verticalAlignment = Alignment.Bottom,
  ) {
    OutlinedTextField(
      value = value,
      onValueChange = onValue,
      placeholder = { Text("Message Hermes…") },
      modifier = Modifier.weight(1f),
      maxLines = 5,
    )
    Spacer(Modifier.size(6.dp))
    if (streaming) {
      IconButton(onClick = onStop) {
        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
      }
    } else {
      IconButton(onClick = onSend, enabled = value.isNotBlank()) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
      }
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
      "Your assistant can read and act on your Ultimate Brain.",
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

package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private sealed interface Md {
  data class H(val level: Int, val text: String) : Md
  data class P(val text: String) : Md
  data class Bullet(val marker: String, val text: String) : Md
  data class Quote(val text: String) : Md
  data class Code(val text: String) : Md
}

private fun parse(markdown: String): List<Md> {
  val lines = markdown.replace("\r\n", "\n").split("\n")
  val out = ArrayList<Md>()
  val para = StringBuilder()
  fun flush() {
    if (para.isNotBlank()) out += Md.P(para.toString().trim())
    para.setLength(0)
  }
  var i = 0
  while (i < lines.size) {
    val line = lines[i]
    when {
      line.startsWith("```") -> {
        flush()
        val code = StringBuilder()
        i++
        while (i < lines.size && !lines[i].startsWith("```")) { code.appendLine(lines[i]); i++ }
        out += Md.Code(code.toString().trimEnd())
      }
      line.startsWith("### ") -> { flush(); out += Md.H(3, line.drop(4)) }
      line.startsWith("## ") -> { flush(); out += Md.H(2, line.drop(3)) }
      line.startsWith("# ") -> { flush(); out += Md.H(1, line.drop(2)) }
      line.startsWith("> ") -> { flush(); out += Md.Quote(line.drop(2)) }
      Regex("^- \\[[ xX]] ").containsMatchIn(line) -> {
        flush()
        val checked = line.contains("[x]", ignoreCase = true)
        out += Md.Bullet(if (checked) "☑" else "☐", line.replace(Regex("^- \\[[ xX]] "), ""))
      }
      line.startsWith("- ") || line.startsWith("* ") -> { flush(); out += Md.Bullet("•", line.drop(2)) }
      Regex("^\\d+\\. ").containsMatchIn(line) -> {
        flush(); out += Md.Bullet(line.substringBefore(".") + ".", line.substringAfter(". "))
      }
      line.isBlank() -> flush()
      else -> para.appendLine(line)
    }
    i++
  }
  flush()
  return out
}

/** Lightweight Markdown renderer for Notion page bodies. Not full CommonMark. */
@Composable
fun MarkdownBody(markdown: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    parse(markdown).forEach { block ->
      when (block) {
        is Md.H -> Text(
          text = inline(block.text),
          style = when (block.level) {
            1 -> MaterialTheme.typography.titleLarge
            2 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
          },
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        is Md.P -> Text(
          text = inline(block.text),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(vertical = 4.dp),
        )
        is Md.Bullet -> Row(modifier = Modifier.padding(vertical = 3.dp)) {
          Text(block.marker, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
          Text(inline(block.text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        is Md.Quote -> Row(modifier = Modifier.padding(vertical = 4.dp)) {
          Surface(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.width(3.dp)) { Spacer(Modifier) }
          Spacer(Modifier.width(10.dp))
          Text(inline(block.text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is Md.Code -> Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.inverseSurface,
          modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
          Text(
            text = block.text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    }
  }
}

private fun inline(s: String): AnnotatedString = buildAnnotatedString {
  var i = 0
  while (i < s.length) {
    when {
      s.startsWith("**", i) -> {
        val end = s.indexOf("**", i + 2)
        if (end > 0) { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.substring(i + 2, end)) }; i = end + 2 }
        else { append(s[i]); i++ }
      }
      s[i] == '*' -> {
        val end = s.indexOf('*', i + 1)
        if (end > 0) { withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.substring(i + 1, end)) }; i = end + 1 }
        else { append(s[i]); i++ }
      }
      s[i] == '`' -> {
        val end = s.indexOf('`', i + 1)
        if (end > 0) { withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(s.substring(i + 1, end)) }; i = end + 1 }
        else { append(s[i]); i++ }
      }
      else -> { append(s[i]); i++ }
    }
  }
}

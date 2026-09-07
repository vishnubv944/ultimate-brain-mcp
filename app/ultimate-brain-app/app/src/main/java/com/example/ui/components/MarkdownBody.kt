package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private sealed interface Md {
  data class H(val level: Int, val text: String) : Md
  data class P(val text: String) : Md
  data class Bullet(val marker: String, val text: String) : Md
  data class Quote(val text: String) : Md
  data class Code(val text: String) : Md
  data object Divider : Md
}

/**
 * Notion's `/pages/{id}/markdown` export wraps every block type the plain
 * Markdown spec can't represent in pseudo-XML tags — `<database>`,
 * `<details>`/`<summary>` toggles, `<table_of_contents/>`, `<callout>`,
 * `<column_list>`, `<child-page>`, `<img>`, `<empty-block/>` … and it
 * backslash-escapes Markdown punctuation. Rendered verbatim that's a wall of
 * tag soup, so fold it down to readable Markdown before parsing.
 */
private fun sanitizeNotionMarkdown(raw: String): String {
  var s = raw.replace("\r\n", "\n")

  // Un-escape Notion's backslash-escaped punctuation (\|  \_  \*  \#  \- …).
  s = Regex("""\\([\\`*_{}\[\]()#+\-.!|<>~])""").replace(s) { it.groupValues[1] }

  // Embedded / linked database views (`<database>`, `<child_database>`, …).
  // These are collection views baked into the page template — the app already
  // renders the real linked Tasks / Notes / etc. as their own sections, so the
  // view here is just noise. Drop it entirely.
  s = Regex(
    """<(?:child[_-])?(?:linked[_-])?database\b[^>]*>[\s\S]*?</(?:child[_-])?(?:linked[_-])?database>""",
    RegexOption.IGNORE_CASE,
  ).replace(s, "")
  s = Regex("""<(?:child[_-])?(?:linked[_-])?database\b[^>]*/?>""", RegexOption.IGNORE_CASE).replace(s, "")

  // <callout icon="X">TEXT</callout>  ->  > X TEXT
  s = Regex("""<callout\b(?:[^>]*\bicon="([^"]*)")?[^>]*>([\s\S]*?)</callout>""", RegexOption.IGNORE_CASE)
    .replace(s) { "> ${(it.groupValues[1] + " " + it.groupValues[2].trim()).trim()}" }

  // <summary>TEXT</summary>  ->  a heading lead-in for the toggle
  s = Regex("""<summary>([\s\S]*?)</summary>""", RegexOption.IGNORE_CASE)
    .replace(s) { "\n### ${it.groupValues[1].replace(Regex("[*_]{1,2}"), "").trim()}\n" }

  // Links to other Notion pages -> just their title (relative URLs can't open).
  s = Regex("""<(?:child-page|link-to-page|page)\b[^>]*>([^<]*)</(?:child-page|link-to-page|page)>""", RegexOption.IGNORE_CASE)
    .replace(s) { it.groupValues[1].trim() }

  // <bookmark url="U" ...>…</bookmark>  ->  the bare URL
  s = Regex("""<bookmark\b[^>]*\burl="([^"]*)"[^>]*>[\s\S]*?</bookmark>""", RegexOption.IGNORE_CASE)
    .replace(s) { it.groupValues[1] }

  // Images: keep the alt text (the renderer doesn't load images).
  s = Regex("""<img\b[^>]*\balt="([^"]+)"[^>]*/?>""", RegexOption.IGNORE_CASE)
    .replace(s) { "🖼 ${it.groupValues[1]}" }
  s = Regex("""<img\b[^>]*/?>""", RegexOption.IGNORE_CASE).replace(s, "")

  // Dividers.
  s = Regex("""<divider\s*/?>""", RegexOption.IGNORE_CASE).replace(s, "\n---\n")

  // Structural wrappers that carry no text of their own — drop the tags,
  // keep whatever was inside.
  s = Regex(
    """</?(?:details|toggle|column_list|columns?|column|synced_block|synced-block|table_of_contents|toc|breadcrumb|empty-block|table|thead|tbody|tr|td|th)\b[^>]*/?>""",
    RegexOption.IGNORE_CASE,
  ).replace(s, "")

  // Anything else that still looks like an XML tag: strip the tag, keep text.
  s = Regex("""</?[a-zA-Z][\w-]*(?:\s+[^>]*?)?/?>""").replace(s, "")

  // Notion internal links are relative ("/<pageid>?pvs=25"); rewrite to an
  // absolute notion.so URL so the link actually opens the page.
  s = Regex("""\(/([0-9a-f]{16,})(?:\?[^)]*)?\)""").replace(s) { "(https://www.notion.so/${it.groupValues[1]})" }
  s = Regex("""\((https://(?:www\.)?notion\.so/[^)?]+)\?[^)]*\)""").replace(s) { "(${it.groupValues[1]})" }

  // Drop "nav chrome" lines — a row that is nothing but page links (and maybe a
  // leading label like "Nav") separated by pipes. UB templates put one at the
  // top of every project / goal page; it's not content.
  s = s.split("\n").filterNot { line ->
    val stripped = line
      .replace(Regex("""\[[^\]]*]\([^)]*\)"""), "")
      .replace(Regex("""[|·*_#>`~\-]"""), "")
      .trim()
    val hadLink = Regex("""\[[^\]]*]\([^)]*\)""").containsMatchIn(line)
    hadLink && (stripped.isEmpty() || stripped.equals("nav", ignoreCase = true))
  }.joinToString("\n")

  return s.replace(Regex("""\n{3,}"""), "\n\n").trim()
}

/**
 * True when [raw] contains actual prose/list/quote content worth showing —
 * not just headings left behind after the template scaffolding (nav row,
 * embedded database views, table of contents) is stripped out. Callers use
 * this to hide an empty "About" section entirely.
 */
fun markdownHasRenderableContent(raw: String): Boolean =
  parse(raw).any { it is Md.P || it is Md.Bullet || it is Md.Quote || it is Md.Code }

private fun parse(markdown: String): List<Md> {
  val lines = sanitizeNotionMarkdown(markdown).split("\n")
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
      line.trim() == "---" || line.trim() == "***" || line.trim() == "___" -> { flush(); out += Md.Divider }
      line.startsWith("### ") -> { flush(); out += Md.H(3, line.drop(4)) }
      line.startsWith("## ") -> { flush(); out += Md.H(2, line.drop(3)) }
      line.startsWith("# ") -> { flush(); out += Md.H(1, line.drop(2)) }
      line.startsWith("> ") -> { flush(); out += Md.Quote(line.drop(2)) }
      Regex("^\\s*- \\[[ xX]] ").containsMatchIn(line) -> {
        flush()
        val checked = line.contains("[x]", ignoreCase = true)
        out += Md.Bullet(if (checked) "☑" else "☐", line.replace(Regex("^\\s*- \\[[ xX]] "), ""))
      }
      line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
        flush(); out += Md.Bullet("•", line.trimStart().drop(2))
      }
      Regex("^\\s*\\d+\\. ").containsMatchIn(line) -> {
        flush(); out += Md.Bullet(line.trimStart().substringBefore(".") + ".", line.substringAfter(". "))
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
  val linkColor = MaterialTheme.colorScheme.primary
  Column(modifier = modifier.fillMaxWidth()) {
    parse(markdown).forEach { block ->
      when (block) {
        is Md.H -> Text(
          text = inline(block.text, linkColor),
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
          text = inline(block.text, linkColor),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(vertical = 4.dp),
        )
        is Md.Bullet -> Row(modifier = Modifier.padding(vertical = 3.dp)) {
          Text(block.marker, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
          Text(inline(block.text, linkColor), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        is Md.Quote -> Row(modifier = Modifier.padding(vertical = 4.dp)) {
          Surface(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.width(3.dp)) { Spacer(Modifier) }
          Spacer(Modifier.width(10.dp))
          Text(inline(block.text, linkColor), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Md.Divider -> HorizontalDivider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )
      }
    }
  }
}

private fun inline(s: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
  var i = 0
  while (i < s.length) {
    when {
      s[i] == '[' -> {
        val close = s.indexOf(']', i + 1)
        val open = if (close > 0 && close + 1 < s.length && s[close + 1] == '(') close + 1 else -1
        val end = if (open > 0) s.indexOf(')', open + 1) else -1
        if (end > 0) {
          val text = s.substring(i + 1, close)
          val url = s.substring(open + 1, end).trim()
          if (url.startsWith("http")) {
            withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)))) {
              append(text)
            }
          } else {
            withStyle(SpanStyle(color = linkColor)) { append(text) }
          }
          i = end + 1
        } else { append(s[i]); i++ }
      }
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

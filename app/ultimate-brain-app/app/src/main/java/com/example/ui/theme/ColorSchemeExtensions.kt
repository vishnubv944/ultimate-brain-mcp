package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Extension properties that surface the parallel entity / functional palette
 * as proper M3 ColorScheme tokens. Each entity color picks its light or dark
 * variant from the active color scheme (lightColorScheme / darkColorScheme in
 * Theme.kt), so screens never reach into Color.kt for an entity color — they
 * go through `MaterialTheme.colorScheme.entityProjects` etc. and dark-mode
 * flips automatically.
 *
 * Audit finding #6 (theme bridge).
 *
 * Naming:
 * - `entity*` — the seven domain entity colors.
 * - `success*` / `error*` / `warning*` — functional colors. We DON'T shadow
 *   the existing M3 `error*` tokens (error / errorContainer / onError / etc.),
 *   so our success/warning families stay separate.
 */

// Light/dark splitter: ColorScheme in material3 1.x doesn't expose `isDark`
// directly. We use the resolved `surface` to detect which palette is live —
// in light schemes surface is near-white, in dark schemes it's the slate
// SurfaceDark. The luminance test is stable across the two palettes we ship.
private val ColorScheme.isDark: Boolean
  get() = surface.luminance() < 0.5f

private fun Color.luminance(): Float =
  // Approximate luminance using the standard sRGB coefficient (matches what
  // Compose's `Color.luminance()` does in compose-ui-graphics 1.7+).
  0.2126f * red + 0.7152f * green + 0.0722f * blue

// Entity colors ----------------------------------------------------------------

val ColorScheme.entityProjects: Color
  get() = if (isDark) EntityProjectsDark else EntityProjects

val ColorScheme.entityTasks: Color
  get() = if (isDark) EntityTasksDark else EntityTasks

val ColorScheme.entityGoals: Color
  get() = if (isDark) EntityGoalsDark else EntityGoals

val ColorScheme.entityNotes: Color
  get() = if (isDark) EntityNotesDark else EntityNotes

val ColorScheme.entityTagArea: Color
  get() = if (isDark) EntityTagAreaDark else EntityTagArea

val ColorScheme.entityTagResource: Color
  get() = if (isDark) EntityTagResourceDark else EntityTagResource

val ColorScheme.entityTagEntity: Color
  get() = if (isDark) EntityTagEntityDark else EntityTagEntity

// Functional colors (success / warning only — error is already an M3 token) --

val ColorScheme.success: Color
  get() = if (isDark) SuccessGreenDark else SuccessGreen

val ColorScheme.successContainer: Color
  get() = if (isDark) SuccessContainerDark else SuccessContainerLight

val ColorScheme.onSuccessContainer: Color
  // In dark mode, container is a dark green; legible text on it is the same
  // SuccessGreenDark's light variant — reuse `success` for that.
  get() = if (isDark) SuccessGreenDark else SuccessGreen

val ColorScheme.warning: Color
  get() = if (isDark) WarningAmberDark else WarningAmber

val ColorScheme.warningContainer: Color
  get() = if (isDark) WarningContainerDark else WarningContainerLight

val ColorScheme.onWarningContainer: Color
  get() = if (isDark) WarningAmberDark else WarningAmber

// Error accent (audit Finding 6): the raw entity error color (ErrorRed) used
// for chips, dots, and severity stripes. M3's built-in `error` token maps to
// the same value but we expose `errorAccent` so call sites can name the
// intent (this color says "severity/error", not "destructive action button"
// which is what the M3 `error` slot is reserved for in M3 spec). Falls back
// to M3's `error` token so dark mode also uses the dark variant already
// wired in Theme.kt's darkColorScheme(error = ErrorRedDark).
val ColorScheme.errorAccent: Color
  get() = if (isDark) ErrorRedDark else ErrorRed

// Note-type tonal chips (audit Finding 11). The NoteTypeBadge composable in
// NotesScreen.kt used to hardcode the container/onContainer pair as raw Color
// literals — these extensions flip the pair with the active scheme so dark
// mode gets a properly tinted container instead of a glowing bright pill.

data class NoteTypeColors(val container: Color, val onContainer: Color)

fun noteTypeColors(type: String, scheme: ColorScheme): NoteTypeColors = when (type) {
  "Meeting" -> NoteTypeColors(
    if (scheme.isDark) NoteMeetingContainerDark else NoteMeetingContainer,
    if (scheme.isDark) NoteMeetingOnContainerDark else NoteMeetingOnContainer,
  )

  "Reference" -> NoteTypeColors(
    if (scheme.isDark) NoteReferenceContainerDark else NoteReferenceContainer,
    if (scheme.isDark) NoteReferenceOnContainerDark else NoteReferenceOnContainer,
  )

  "Idea" -> NoteTypeColors(
    if (scheme.isDark) NoteIdeaContainerDark else NoteIdeaContainer,
    if (scheme.isDark) NoteIdeaOnContainerDark else NoteIdeaOnContainer,
  )

  "Journal" -> NoteTypeColors(
    if (scheme.isDark) NoteJournalContainerDark else NoteJournalContainer,
    if (scheme.isDark) NoteJournalOnContainerDark else NoteJournalOnContainer,
  )

  "Book" -> NoteTypeColors(
    if (scheme.isDark) NoteBookContainerDark else NoteBookContainer,
    if (scheme.isDark) NoteBookOnContainerDark else NoteBookOnContainer,
  )

  else -> NoteTypeColors(scheme.surfaceContainerHigh, scheme.onSurface)
}

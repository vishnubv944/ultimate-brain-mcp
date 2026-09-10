package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Indigo Palette
val PrimaryIndigo = Color(0xFF4F46E5)
val PrimaryIndigoDark = Color(0xFFAAB4FF)     // periwinkle — vivid, not washed lavender
val PrimaryContainerLight = Color(0xFFE0E7FF)
val PrimaryContainerDark = Color(0xFF34386E)
val OnPrimaryContainerLight = Color(0xFF181A3D)
val OnPrimaryContainerDark = Color(0xFFDFE1FF)

// Secondary — pulled into the indigo family (was cyan, which clashed).
val SecondaryBlue = Color(0xFF006591)
val SecondaryBlueDark = Color(0xFFB9C0E8)
val SecondaryContainerLight = Color(0xFFE0F2FE)
val SecondaryContainerDark = Color(0xFF2C3052)
val OnSecondaryContainerLight = Color(0xFF004666)
val OnSecondaryContainerDark = Color(0xFFD6DBFF)

// Surfaces & Backgrounds - Light
val BackgroundLight = Color(0xFFFAF8FF)
val OnBackgroundLight = Color(0xFF131B2E)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF131B2E)
val OnSurfaceVariantLight = Color(0xFF464555)
val SurfaceContainerLight = Color(0xFFEAEDFF)
val SurfaceContainerLowLight = Color(0xFFF2F3FF)
val SurfaceContainerHighLight = Color(0xFFE2E8F0)
val OutlineLight = Color(0xFF94A3B8)
val OutlineVariantLight = Color(0xFFCBD5E1)

// Surfaces & Backgrounds - Dark — warm graphite, not cold slate-navy.
val BackgroundDark = Color(0xFF0F1013)
val OnBackgroundDark = Color(0xFFE9EAEC)
val SurfaceDark = Color(0xFF131417)
val OnSurfaceDark = Color(0xFFE9EAEC)
val OnSurfaceVariantDark = Color(0xFF9B9EA6)
val SurfaceContainerDark = Color(0xFF1C1E22)
val SurfaceContainerLowDark = Color(0xFF17181C)
val SurfaceContainerHighDark = Color(0xFF24262B)
val OutlineDark = Color(0xFF3C3F46)
val OutlineVariantDark = Color(0xFF26282D)

// Functional & Entity Accents
// Spec values come from app-spec/04-design-tokens.md. Three values were
// previously drifted: Tasks was #0284C7 (blue 600), TagArea was #0D9488
// (teal 600), TagResource was #3B82F6 (blue 500 — collides with Tasks!).
// Round 4 audit fix #14 brings them in line with the spec.
val EntityProjects = Color(0xFF8B5CF6)
val EntityProjectsDark = Color(0xFFBBA9FF)
val EntityTasks = Color(0xFF3B82F6)             // Spec: blue 500
val EntityTasksDark = Color(0xFF93C5FD)
val EntityTagArea = Color(0xFF14B8A6)            // Spec: teal 500
val EntityTagAreaDark = Color(0xFF5EEAD4)
val EntityTagResource = Color(0xFF06B6D4)        // Spec: cyan 500
val EntityTagResourceDark = Color(0xFF67E8F9)
val EntityTagEntity = Color(0xFFEC4899)
val EntityTagEntityDark = Color(0xFFF472B6)
val EntityGoals = Color(0xFF10B981)
val EntityGoalsDark = Color(0xFF6EE7B7)
val EntityNotes = Color(0xFFF59E0B)
val EntityNotesDark = Color(0xFFFCD34D)

val SuccessGreen = Color(0xFF16A34A)
val SuccessGreenDark = Color(0xFF86EFAC)
val SuccessContainerLight = Color(0xFFDCFCE7)
val SuccessContainerDark = Color(0xFF14532D)

val ErrorRed = Color(0xFFDC2626)
val ErrorRedDark = Color(0xFFF98787)          // crisper overdue red on graphite
val ErrorContainerLight = Color(0xFFFFDAD6)
val ErrorContainerDark = Color(0xFF5C1A1A)

val WarningAmber = Color(0xFFD97706)
val WarningAmberDark = Color(0xFFFCD34D)
val WarningContainerLight = Color(0xFFFEF3C7)
val WarningContainerDark = Color(0xFF78350F)

// Note-type tonal chips (audit Finding 11). Each pair is the standard M3
// "container / on-container" shape: a pale tinted background with a deeply
// saturated foreground. Light/dark variants are written explicitly because the
// container flip isn't a simple inversion — dark containers are tinted, not
// diluted versions of the foreground.
val NoteMeetingContainer = Color(0xFFE0E7FF)
val NoteMeetingOnContainer = Color(0xFF3730A3)
val NoteMeetingContainerDark = Color(0xFF1E1B4B)
val NoteMeetingOnContainerDark = Color(0xFFC7D2FE)

val NoteReferenceContainer = Color(0xFFF3E8FF)
val NoteReferenceOnContainer = Color(0xFF6B21A8)
val NoteReferenceContainerDark = Color(0xFF4C1D95)
val NoteReferenceOnContainerDark = Color(0xFFDDD6FE)

val NoteIdeaContainer = Color(0xFFFEF3C7)
val NoteIdeaOnContainer = Color(0xFF92400E)
val NoteIdeaContainerDark = Color(0xFF78350F)
val NoteIdeaOnContainerDark = Color(0xFFFCD34D)

val NoteJournalContainer = Color(0xFFD1FAE5)
val NoteJournalOnContainer = Color(0xFF065F46)
val NoteJournalContainerDark = Color(0xFF064E3B)
val NoteJournalOnContainerDark = Color(0xFFA7F3D0)

val NoteBookContainer = Color(0xFFFFE4E6)
val NoteBookOnContainer = Color(0xFF9F1239)
val NoteBookContainerDark = Color(0xFF881337)
val NoteBookOnContainerDark = Color(0xFFFECDD3)

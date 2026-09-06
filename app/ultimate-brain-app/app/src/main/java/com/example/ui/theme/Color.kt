package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Indigo Palette
val PrimaryIndigo = Color(0xFF4F46E5)
val PrimaryIndigoDark = Color(0xFFA5B4FC)
val PrimaryContainerLight = Color(0xFFE0E7FF)
val PrimaryContainerDark = Color(0xFF312E81)
val OnPrimaryContainerLight = Color(0xFF1E1B4B)
val OnPrimaryContainerDark = Color(0xFFE0E7FF)

// Secondary Cyan/Blue Palette
val SecondaryBlue = Color(0xFF006591)
val SecondaryBlueDark = Color(0xFF7DD3FC)
val SecondaryContainerLight = Color(0xFFE0F2FE)
val SecondaryContainerDark = Color(0xFF0C4A6E)
val OnSecondaryContainerLight = Color(0xFF004666)
val OnSecondaryContainerDark = Color(0xFFE0F2FE)

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

// Surfaces & Backgrounds - Dark
val BackgroundDark = Color(0xFF0B1220)
val OnBackgroundDark = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF0F172A)
val OnSurfaceDark = Color(0xFFF8FAFC)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val SurfaceContainerDark = Color(0xFF1E293B)
val SurfaceContainerLowDark = Color(0xFF0B1220)
val SurfaceContainerHighDark = Color(0xFF334155)
val OutlineDark = Color(0xFF64748B)
val OutlineVariantDark = Color(0xFF334155)

// Functional & Entity Accents
// Spec values come from app-spec/04-design-tokens.md. Three values were
// previously drifted: Tasks was #0284C7 (blue 600), TagArea was #0D9488
// (teal 600), TagResource was #3B82F6 (blue 500 — collides with Tasks!).
// Round 4 audit fix #14 brings them in line with the spec.
val EntityProjects = Color(0xFF8B5CF6)
val EntityProjectsDark = Color(0xFFC4B5FD)
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
val ErrorRedDark = Color(0xFFFCA5A5)
val ErrorContainerLight = Color(0xFFFFDAD6)
val ErrorContainerDark = Color(0xFF7F1D1D)

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

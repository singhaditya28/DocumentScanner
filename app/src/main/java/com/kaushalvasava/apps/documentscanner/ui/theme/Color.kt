package com.kaushalvasava.apps.documentscanner.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Neutral palette (reused across schemes) ─────────────────────────────────
val White = Color(0xFFFFFFFF)
val NearWhite = Color(0xFFF9FAFB)       // --background light (hsl 210 0% 98%)
val SlateGray = Color(0xFFE2E8F0)       // --border
val MutedForeground = Color(0xFF64748B) // --muted-foreground
val ErrorRed = Color(0xFFEF4444)        // --destructive (same across all themes)
val ErrorRedDark = Color(0xFFFCA5A5)

// ─── Default (Dark Navy) ─────────────────────────────────────────────────────
// Web: --primary: 222.2 47.4% 11.2%  (hsl → #0F172A)
val DefaultPrimary = Color(0xFF0F172A)
val DefaultPrimaryContainer = Color(0xFFE0E7FF)
val DefaultOnPrimary = White
val DefaultOnPrimaryContainer = Color(0xFF0F172A)
val DefaultSecondary = Color(0xFF334155)
val DefaultSecondaryContainer = Color(0xFFEFF4FF)
val DefaultOnSecondary = White
val DefaultOnSecondaryContainer = Color(0xFF1E293B)
val DefaultBackground = NearWhite
val DefaultSurface = White
val DefaultOnBackground = Color(0xFF0F172A)
val DefaultOnSurface = Color(0xFF0F172A)
val DefaultSurfaceVariant = Color(0xFFF1F5F9)
val DefaultOnSurfaceVariant = MutedForeground
val DefaultOutline = SlateGray
val DefaultTertiary = Color(0xFF4F46E5)
val DefaultTertiaryContainer = Color(0xFFEEF2FF)
val DefaultOnTertiary = White
val DefaultOnTertiaryContainer = Color(0xFF312E81)

// Dark mode for Default
val DefaultDarkPrimary = Color(0xFFE2E8F0)
val DefaultDarkPrimaryContainer = Color(0xFF1E293B)
val DefaultDarkOnPrimary = Color(0xFF0C1222)
val DefaultDarkOnPrimaryContainer = Color(0xFFCBD5E1)
val DefaultDarkBackground = Color(0xFF0C1222)   // --background dark hsl(222 84% 4.9%)
val DefaultDarkSurface = Color(0xFF111827)
val DefaultDarkOnBackground = Color(0xFFF8FAFC)
val DefaultDarkOnSurface = Color(0xFFF1F5F9)
val DefaultDarkSurfaceVariant = Color(0xFF1E293B)
val DefaultDarkOnSurfaceVariant = Color(0xFF94A3B8)
val DefaultDarkOutline = Color(0xFF334155)
val DefaultDarkSecondary = Color(0xFF94A3B8)
val DefaultDarkSecondaryContainer = Color(0xFF1E293B)
val DefaultDarkOnSecondary = Color(0xFF0C1222)
val DefaultDarkOnSecondaryContainer = Color(0xFFCBD5E1)
val DefaultDarkTertiary = Color(0xFF818CF8)
val DefaultDarkTertiaryContainer = Color(0xFF1E1B4B)
val DefaultDarkOnTertiary = Color(0xFF1E1B4B)
val DefaultDarkOnTertiaryContainer = Color(0xFFC7D2FE)

// ─── Miracle Orange ──────────────────────────────────────────────────────────
// Web: --primary: 24 100% 50% = #FF6600
val OrangePrimary = Color(0xFFFF6600)
val OrangePrimaryContainer = Color(0xFFFFEBD5)
val OrangeOnPrimary = White
val OrangeOnPrimaryContainer = Color(0xFF7C2D00)
val OrangeSecondary = Color(0xFFEA580C)
val OrangeSecondaryContainer = Color(0xFFFFF0E4)
val OrangeOnSecondary = White
val OrangeOnSecondaryContainer = Color(0xFF7C2D00)
val OrangeBackground = Color(0xFFFFF8F3)          // --background: 30 100% 97%
val OrangeSurface = Color(0xFFFFFBF8)
val OrangeOnBackground = Color(0xFF1A0A00)
val OrangeOnSurface = Color(0xFF1A0A00)
val OrangeSurfaceVariant = Color(0xFFFFEDD5)
val OrangeOnSurfaceVariant = Color(0xFF78350F)
val OrangeOutline = Color(0xFFFBD0A8)
val OrangeTertiary = Color(0xFFD97706)
val OrangeTertiaryContainer = Color(0xFFFEF3C7)
val OrangeOnTertiary = White
val OrangeOnTertiaryContainer = Color(0xFF78350F)

// Dark orange
val OrangeDarkPrimary = Color(0xFFFFAA6E)
val OrangeDarkBackground = Color(0xFF1A0A00)
val OrangeDarkSurface = Color(0xFF2D1200)
val OrangeDarkPrimaryContainer = Color(0xFF7C2D00)
val OrangeDarkOnPrimary = Color(0xFF3D1500)
val OrangeDarkOnBackground = Color(0xFFFFDDC1)
val OrangeDarkOnSurface = Color(0xFFFFDDC1)

// ─── Ocean Blue ───────────────────────────────────────────────────────────────
// Web: --primary: 210 100% 45% = #0077E6
val BluePrimary = Color(0xFF0077E6)
val BluePrimaryContainer = Color(0xFFDCEEFF)
val BlueOnPrimary = White
val BlueOnPrimaryContainer = Color(0xFF003870)
val BlueSecondary = Color(0xFF0369A1)
val BlueSecondaryContainer = Color(0xFFE0F2FE)
val BlueOnSecondary = White
val BlueOnSecondaryContainer = Color(0xFF075985)
val BlueBackground = Color(0xFFF2F8FF)           // --background: 210 100% 97%
val BlueSurface = Color(0xFFF8FBFF)
val BlueOnBackground = Color(0xFF001B38)
val BlueOnSurface = Color(0xFF001B38)
val BlueSurfaceVariant = Color(0xFFDCEEFF)
val BlueOnSurfaceVariant = Color(0xFF075985)
val BlueOutline = Color(0xFFBAD8F5)
val BlueTertiary = Color(0xFF0284C7)
val BlueTertiaryContainer = Color(0xFFE0F2FE)
val BlueOnTertiary = White
val BlueOnTertiaryContainer = Color(0xFF075985)

// Dark blue
val BlueDarkPrimary = Color(0xFF60B4FF)
val BlueDarkBackground = Color(0xFF001B38)
val BlueDarkSurface = Color(0xFF002855)
val BlueDarkPrimaryContainer = Color(0xFF003870)
val BlueDarkOnPrimary = Color(0xFF00356C)
val BlueDarkOnBackground = Color(0xFFD0E8FF)
val BlueDarkOnSurface = Color(0xFFD0E8FF)

// ─── Forest Green ─────────────────────────────────────────────────────────────
// Web: --primary: 140 60% 40% = #299954
val GreenPrimary = Color(0xFF299954)
val GreenPrimaryContainer = Color(0xFFDCFCE7)
val GreenOnPrimary = White
val GreenOnPrimaryContainer = Color(0xFF14532D)
val GreenSecondary = Color(0xFF16A34A)
val GreenSecondaryContainer = Color(0xFFF0FDF4)
val GreenOnSecondary = White
val GreenOnSecondaryContainer = Color(0xFF14532D)
val GreenBackground = Color(0xFFF4FDF6)          // --background: 140 50% 97%
val GreenSurface = Color(0xFFFAFFFB)
val GreenOnBackground = Color(0xFF052E16)
val GreenOnSurface = Color(0xFF052E16)
val GreenSurfaceVariant = Color(0xFFDCFCE7)
val GreenOnSurfaceVariant = Color(0xFF166534)
val GreenOutline = Color(0xFFBBF7D0)
val GreenTertiary = Color(0xFF0D9488)
val GreenTertiaryContainer = Color(0xFFCCFBF1)
val GreenOnTertiary = White
val GreenOnTertiaryContainer = Color(0xFF134E4A)

// Dark green
val GreenDarkPrimary = Color(0xFF86EFAC)
val GreenDarkBackground = Color(0xFF052E16)
val GreenDarkSurface = Color(0xFF0A3D22)
val GreenDarkPrimaryContainer = Color(0xFF14532D)
val GreenDarkOnPrimary = Color(0xFF052E16)
val GreenDarkOnBackground = Color(0xFFBBF7D0)
val GreenDarkOnSurface = Color(0xFFBBF7D0)
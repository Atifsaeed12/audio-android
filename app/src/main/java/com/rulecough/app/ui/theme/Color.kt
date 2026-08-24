package com.rulecough.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Viridis scale (used for brand + the probability chart) ----
val Viridis0 = Color(0xFF440154)
val Viridis1 = Color(0xFF443983)
val Viridis2 = Color(0xFF31688E)
val Viridis3 = Color(0xFF21918C)
val Viridis4 = Color(0xFF35B779)
val Viridis5 = Color(0xFF90D743)
val Viridis6 = Color(0xFFFDE725)

// Ordered palette sampled for up to 7 classes.
val ViridisScale = listOf(Viridis2, Viridis3, Viridis4, Viridis1, Viridis5, Viridis0, Viridis6)

// ---- Semantic (risk only — deliberately separate from the accent) ----
val RiskLow = Color(0xFF1F9D6B)
val RiskModerate = Color(0xFFC98A00)
val RiskHigh = Color(0xFFD1495B)

// ---- Light theme neutrals (cool, slight purple bias) ----
val LightBg = Color(0xFFEEF0F6)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF3F4FA)
val LightOutline = Color(0xFFE0E0EC)
val LightInk = Color(0xFF1B1930)
val LightMuted = Color(0xFF5C5A72)

// ---- Dark theme neutrals (deep purple-black) ----
val DarkBg = Color(0xFF100E1A)
val DarkSurface = Color(0xFF1C1930)
val DarkSurfaceVariant = Color(0xFF242041)
val DarkOutline = Color(0xFF2E2A48)
val DarkInk = Color(0xFFECEBF6)
val DarkMuted = Color(0xFFA7A4C2)

val DarkAccent = Color(0xFF34C2A8)

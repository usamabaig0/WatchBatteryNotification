package pl.example.bateria.ui.theme

import androidx.compose.ui.graphics.Color
import pl.example.bateria.core.model.AccentColor

// Dark Palette (Section 3.1 of DESIGN.md)
val DarkBackground = Color(0xFF101214)
val DarkSurface = Color(0xFF181B1F)
val DarkSurfaceElevated = Color(0xFF20242A)
val DarkBorder = Color(0xFF2B3138)
val DarkTextPrimary = Color(0xFFF2F4F7)
val DarkTextSecondary = Color(0xFFA7AFB8)
val DarkTextDisabled = Color(0xFF626A73)
val DarkAccentDefault = Color(0xFF6AAFE4)
val DarkAccentSoft = Color(0xFF1D2C3A)

// Light Palette (Section 3.1 of DESIGN.md)
val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF0F3F6)
val LightBorder = Color(0xFFDCE2E7)
val LightTextPrimary = Color(0xFF17191C)
val LightTextSecondary = Color(0xFF69727D)
val LightTextDisabled = Color(0xFF9AA3AC)
val LightAccentDefault = Color(0xFF3B82C4)
val LightAccentSoft = Color(0xFFE7F1FA)

// Status Colors (Section 3.2 of DESIGN.md - Strict Semantic Rules)
val StatusNormal = Color(0xFF3B82C4)
val StatusConnected = Color(0xFF3E9B68) // Success Green
val StatusConnecting = Color(0xFFC58A26) // Warning Amber
val StatusDisconnected = Color(0xFFC95158) // Critical Red
val StatusCharging = Color(0xFF3B82C4) // Primary Blue
val StatusHighBattery = Color(0xFF3E9B68)
val StatusLowBattery = Color(0xFFC58A26)
val StatusCritical = Color(0xFFC95158)
val StatusDisabled = Color(0xFF9AA3AC)

// 5 User-Selectable Accents (Section 3.3 of DESIGN.md)
fun getAccentColor(accent: AccentColor, isDark: Boolean): Color {
    return when (accent) {
        AccentColor.PROFESSIONAL_BLUE -> if (isDark) Color(0xFF6AAFE4) else Color(0xFF3B82C4)
        AccentColor.AMBER_GOLD -> Color(0xFFD49A2A)
        AccentColor.LAVA_RED -> Color(0xFFC95158)
        AccentColor.BALANCED_GREEN -> Color(0xFF3E9B68)
        AccentColor.REFINED_PURPLE -> Color(0xFF8064B5)
    }
}

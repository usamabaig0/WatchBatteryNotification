package pl.example.bateria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import pl.example.bateria.core.model.AccentColor
import pl.example.bateria.core.model.ColorTheme

data class WearBatteryColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val accent: Color,
    val accentSoft: Color,
    val isDark: Boolean
)

@Composable
fun WatchBatteryTheme(
    colorTheme: ColorTheme = ColorTheme.SYSTEM,
    accentColor: AccentColor = AccentColor.PROFESSIONAL_BLUE,
    content: @Composable () -> Unit
) {
    val isDark = when (colorTheme) {
        ColorTheme.SYSTEM -> isSystemInDarkTheme()
        ColorTheme.LIGHT -> false
        ColorTheme.DARK -> true
    }

    val activeAccent = getAccentColor(accentColor, isDark)

    val customColors = if (isDark) {
        WearBatteryColors(
            background = DarkBackground,
            surface = DarkSurface,
            surfaceElevated = DarkSurfaceElevated,
            border = DarkBorder,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textDisabled = DarkTextDisabled,
            accent = activeAccent,
            accentSoft = DarkAccentSoft,
            isDark = true
        )
    } else {
        WearBatteryColors(
            background = LightBackground,
            surface = LightSurface,
            surfaceElevated = LightSurfaceElevated,
            border = LightBorder,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textDisabled = LightTextDisabled,
            accent = activeAccent,
            accentSoft = LightAccentSoft,
            isDark = false
        )
    }

    val materialColorScheme = if (isDark) {
        darkColorScheme(
            background = customColors.background,
            surface = customColors.surface,
            primary = customColors.accent,
            onBackground = customColors.textPrimary,
            onSurface = customColors.textPrimary,
            outline = customColors.border
        )
    } else {
        lightColorScheme(
            background = customColors.background,
            surface = customColors.surface,
            primary = customColors.accent,
            onBackground = customColors.textPrimary,
            onSurface = customColors.textPrimary,
            outline = customColors.border
        )
    }

    MaterialTheme(
        colorScheme = materialColorScheme,
        content = content
    )
}

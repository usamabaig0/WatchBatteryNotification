package com.watchbattery.notification.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 8dp Spacing System (Section 5 of DESIGN.md)
object WearBatterySpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

// Corner Radii (Section 6 of DESIGN.md)
object WearBatteryRadius {
    val card = 16.dp
    val hero = 24.dp
    val button = 12.dp
    val input = 12.dp
    val dialog = 24.dp
    val pill = 9999.dp
}

// Typography Scale (Section 4 of DESIGN.md)
object WearBatteryTypography {
    val heroBattery = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 68.sp,
        letterSpacing = (-0.03).sp
    )
    val wearHeroBattery = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        letterSpacing = (-0.02).sp
    )
    val screenTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    )
    val appTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    )
    val sectionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
    val telemetryValue = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
    val body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    )
    val secondary = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    )
    val label = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
}

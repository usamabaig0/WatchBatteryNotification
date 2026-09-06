package com.watchbattery.notification.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.watchbattery.notification.ui.theme.*

@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(WearBatteryRadius.card),
        color = DarkSurface,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WearBatteryRadius.card))
            .border(1.dp, DarkBorder, RoundedCornerShape(WearBatteryRadius.card))
    ) {
        Column(
            modifier = Modifier
                .padding(WearBatterySpacing.lg),
            content = content
        )
    }
}

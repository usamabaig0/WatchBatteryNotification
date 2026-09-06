package com.watchbattery.notification.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchbattery.notification.core.model.ConnectionStatus
import com.watchbattery.notification.ui.theme.*

@Composable
fun ConnectionStatusPill(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    watchBatteryLevel: Int = 0
) {
    val colors = WearBatteryTheme.colors
    val (dotColor, text) = when (status) {
        ConnectionStatus.CONNECTED -> {
            if (watchBatteryLevel < 0) {
                Pair(StatusConnecting, "Connected (Awaiting Sync)")
            } else {
                Pair(StatusConnected, "Connected")
            }
        }
        ConnectionStatus.CONNECTING -> Pair(StatusConnecting, "Connecting...")
        ConnectionStatus.DISCONNECTED -> Pair(StatusDisconnected, "Disconnected")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(WearBatteryRadius.pill))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(WearBatteryRadius.pill))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = colors.textPrimary,
            fontSize = 12.sp,
            style = WearBatteryTypography.label
        )
    }
}

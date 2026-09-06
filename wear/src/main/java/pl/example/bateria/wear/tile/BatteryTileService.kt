package pl.example.bateria.wear.tile

import android.content.Context
import androidx.wear.tiles.*
import androidx.wear.tiles.LayoutElementBuilders.*
import androidx.wear.tiles.TimelineBuilders.*
import androidx.wear.tiles.RequestBuilders.*
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.protolayout.ResourceBuilders as ProtoResourceBuilders
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class BatteryTileService : TileService() {

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        val prefs = getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
        val level = prefs.getInt("last_battery_level", 82)
        val isCharging = prefs.getBoolean("last_charging_state", false)

        val statusText = if (isCharging) "Charging" else "Discharging"

        val layout = Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .addContent(
                Column.Builder()
                    .addContent(
                        Text.Builder()
                            .setText("$level%")
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(36f))
                                    .setColor(ColorBuilders.argb(0xFFF2F4F7.toInt()))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        Text.Builder()
                            .setText(statusText)
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(14f))
                                    .setColor(ColorBuilders.argb(0xFF6AAFE4.toInt()))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val tile = Tile.Builder()
            .setResourcesVersion("1")
            .setTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(Layout.Builder().setRoot(layout).build())
                            .build()
                    )
                    .build()
            )
            .build()

        return ImmediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ProtoResourceBuilders.Resources> {
        val resources = ProtoResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
        return ImmediateFuture(resources)
    }

    private class ImmediateFuture<T>(private val value: T) : ListenableFuture<T> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = true
        override fun get(): T = value
        override fun get(timeout: Long, unit: TimeUnit): T = value
        override fun addListener(listener: Runnable, executor: Executor) {
            executor.execute(listener)
        }
    }
}

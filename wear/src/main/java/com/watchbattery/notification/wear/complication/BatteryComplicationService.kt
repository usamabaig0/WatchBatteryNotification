package com.watchbattery.notification.wear.complication

import android.content.Context
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

class BatteryComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val bm = getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val sysLevel = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val level = if (sysLevel in 0..100) sysLevel else 100
        return createComplicationData(level, type)
    }

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        val prefs = getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
        val bm = getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val sysLevel = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val level = if (sysLevel in 0..100) sysLevel else prefs.getInt("last_battery_level", 100)
        val data = createComplicationData(level, request.complicationType)
        listener.onComplicationData(data)
    }

    private fun createComplicationData(level: Int, type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = level.toFloat(),
                    min = 0f,
                    max = 100f,
                    contentDescription = PlainComplicationText.Builder("Watch Battery").build()
                )
                    .setText(PlainComplicationText.Builder("$level%").build())
                    .setTitle(PlainComplicationText.Builder("BAT").build())
                    .build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("$level%").build(),
                    contentDescription = PlainComplicationText.Builder("Watch Battery").build()
                )
                    .setTitle(PlainComplicationText.Builder("BAT").build())
                    .build()
            }
            else -> null
        }
    }
}

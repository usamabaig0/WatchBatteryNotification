package pl.example.bateria.wear.complication

import android.content.Context
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

class BatteryComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(82, type)
    }

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        val prefs = getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
        val level = prefs.getInt("last_battery_level", 82)
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

package pl.example.bateria.wear

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class WearBatteryApp : Application() {

    lateinit var prefs: SharedPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences("wear_battery_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        lateinit var instance: WearBatteryApp
            private set
    }
}

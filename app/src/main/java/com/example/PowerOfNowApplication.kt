package com.example

import android.app.Application
import com.example.service.AlarmScheduler
import com.example.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerOfNowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        // Asynchronously sync and schedule the rolling window
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler.rescheduleRollingWindow(this@PowerOfNowApplication)
            } catch (_: Exception) {}
        }
    }
}

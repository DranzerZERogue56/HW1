package com.example.globalmute.domain

import android.app.NotificationManager

class DndControllerImpl(private val notificationManager: NotificationManager) : DndController {

    override fun setFilter(mode: DndMode) {
        val filter = when (mode) {
            DndMode.ALL -> NotificationManager.INTERRUPTION_FILTER_ALL
            DndMode.NONE -> NotificationManager.INTERRUPTION_FILTER_NONE
        }
        notificationManager.setInterruptionFilter(filter)
    }
}

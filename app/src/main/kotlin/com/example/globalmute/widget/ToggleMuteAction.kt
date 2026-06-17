package com.example.globalmute.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.core.content.ContextCompat
import com.example.globalmute.data.MuteRepository
import com.example.globalmute.service.GlobalMuteService
import kotlinx.coroutines.flow.first

class ToggleMuteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val repo = MuteRepository(context)
        val newMuted = !repo.isMuted.first()
        repo.setMuted(newMuted)

        val serviceIntent = Intent(context, GlobalMuteService::class.java).apply {
            action = if (newMuted) GlobalMuteService.ACTION_MUTE else GlobalMuteService.ACTION_UNMUTE
        }
        if (newMuted) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        GlanceAppWidgetManager(context)
            .getGlanceIds(MuteWidget::class.java)
            .forEach { id -> MuteWidget().update(context, id) }
    }
}

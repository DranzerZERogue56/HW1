package com.example.globalmute.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.globalmute.R
import com.example.globalmute.data.MuteRepository
import com.example.globalmute.service.GlobalMuteService
import com.example.globalmute.widget.MuteWidget
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MuteTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        observeJob = scope.launch {
            MuteRepository(applicationContext).isMuted.collect { isMuted ->
                qsTile?.apply {
                    state = if (isMuted) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    label = if (isMuted) "Muted" else "Unmuted"
                    icon = Icon.createWithResource(
                        this@MuteTileService,
                        if (isMuted) R.drawable.ic_muted else R.drawable.ic_unmuted,
                    )
                    updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        observeJob?.cancel()
        observeJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val repo = MuteRepository(applicationContext)
            val newMuted = !repo.isMuted.first()
            repo.setMuted(newMuted)

            val serviceIntent = Intent(this@MuteTileService, GlobalMuteService::class.java).apply {
                action = if (newMuted) GlobalMuteService.ACTION_MUTE else GlobalMuteService.ACTION_UNMUTE
            }
            if (newMuted) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            GlanceAppWidgetManager(applicationContext)
                .getGlanceIds(MuteWidget::class.java)
                .forEach { id -> MuteWidget().update(applicationContext, id) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

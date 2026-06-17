package com.example.globalmute.service

import android.content.ComponentName
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.globalmute.data.MuteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MuteNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeSessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private lateinit var muteRepository: MuteRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        muteRepository = MuteRepository(applicationContext)
        val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSessionManager = msm

        scope.launch {
            muteRepository.isMuted.collect { isMuted ->
                if (isMuted) {
                    pauseAllActiveSessions(msm)
                    startMonitoringNewSessions(msm)
                } else {
                    stopMonitoringNewSessions(msm)
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            if (muteRepository.isMuted.first()) {
                cancelNotification(sbn.key)
            }
        }
    }

    private fun pauseAllActiveSessions(msm: MediaSessionManager) {
        try {
            msm.getActiveSessions(
                ComponentName(this, MuteNotificationListener::class.java),
            ).forEach { controller ->
                controller.transportControls?.pause()
            }
        } catch (_: SecurityException) {
        }
    }

    private fun startMonitoringNewSessions(msm: MediaSessionManager) {
        stopMonitoringNewSessions(msm)
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
            sessions?.forEach { it.transportControls?.pause() }
        }
        activeSessionsListener = listener
        try {
            msm.addOnActiveSessionsChangedListener(
                listener,
                ComponentName(this, MuteNotificationListener::class.java),
            )
        } catch (_: SecurityException) {
        }
    }

    private fun stopMonitoringNewSessions(msm: MediaSessionManager) {
        activeSessionsListener?.let { msm.removeOnActiveSessionsChangedListener(it) }
        activeSessionsListener = null
    }

    override fun onListenerDisconnected() {
        mediaSessionManager?.let { stopMonitoringNewSessions(it) }
        scope.cancel()
        super.onListenerDisconnected()
    }
}

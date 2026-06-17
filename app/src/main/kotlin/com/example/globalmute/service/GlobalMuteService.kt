package com.example.globalmute.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.globalmute.MainActivity
import com.example.globalmute.R
import com.example.globalmute.data.MuteRepository
import com.example.globalmute.domain.AudioControllerImpl
import com.example.globalmute.domain.DndControllerImpl
import com.example.globalmute.domain.MuteLogic
import com.example.globalmute.widget.MuteWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GlobalMuteService : Service() {

    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var muteLogic: MuteLogic
    private lateinit var muteRepository: MuteRepository
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        muteLogic = MuteLogic(
            AudioControllerImpl(audioManager),
            DndControllerImpl(notificationManager),
        )
        muteRepository = MuteRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MUTE -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                scope.launch { handleMute() }
            }
            ACTION_UNMUTE -> scope.launch { handleUnmute() }
        }
        return START_NOT_STICKY
    }

    private suspend fun handleMute() {
        val snapshot = muteLogic.captureVolumes()
        muteRepository.saveVolumes(snapshot)
        muteLogic.applyMute()
        MuteWidget().updateAll(applicationContext)
    }

    private suspend fun handleUnmute() {
        val snapshot = muteRepository.getSavedVolumes() ?: defaultVolumeSnapshot()
        muteLogic.restoreVolumes(snapshot)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        MuteWidget().updateAll(applicationContext)
    }

    private fun defaultVolumeSnapshot() = com.example.globalmute.data.VolumeSnapshot(
        ring = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2,
        notification = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) / 2,
        system = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM) / 2,
        music = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2,
        alarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) / 2,
    )

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Global Mute Status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when global mute is active"
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Global silence is active")
            .setContentText("Tap to open GlobalMute")
            .setSmallIcon(R.drawable.ic_muted)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "globalmute_foreground"
        const val ACTION_MUTE = "com.example.globalmute.ACTION_MUTE"
        const val ACTION_UNMUTE = "com.example.globalmute.ACTION_UNMUTE"
        const val NOTIFICATION_ID = 1
    }
}

package com.example.globalmute.domain

import android.media.AudioManager

class AudioControllerImpl(private val audioManager: AudioManager) : AudioController {

    override fun getVolume(stream: MuteStream): Int =
        audioManager.getStreamVolume(stream.toAndroidStream())

    override fun setVolume(stream: MuteStream, volume: Int) {
        audioManager.setStreamVolume(stream.toAndroidStream(), volume, 0)
    }

    private fun MuteStream.toAndroidStream(): Int = when (this) {
        MuteStream.RING -> AudioManager.STREAM_RING
        MuteStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
        MuteStream.SYSTEM -> AudioManager.STREAM_SYSTEM
        MuteStream.MUSIC -> AudioManager.STREAM_MUSIC
        MuteStream.ALARM -> AudioManager.STREAM_ALARM
    }
}

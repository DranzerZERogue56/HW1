package com.example.globalmute.domain

enum class MuteStream { RING, NOTIFICATION, SYSTEM, MUSIC, ALARM }

interface AudioController {
    fun getVolume(stream: MuteStream): Int
    fun setVolume(stream: MuteStream, volume: Int)
}

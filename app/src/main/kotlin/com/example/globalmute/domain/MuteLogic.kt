package com.example.globalmute.domain

import com.example.globalmute.data.VolumeSnapshot

class MuteLogic(
    private val audioController: AudioController,
    private val dndController: DndController,
) {
    fun captureVolumes(): VolumeSnapshot = VolumeSnapshot(
        ring = audioController.getVolume(MuteStream.RING),
        notification = audioController.getVolume(MuteStream.NOTIFICATION),
        system = audioController.getVolume(MuteStream.SYSTEM),
        music = audioController.getVolume(MuteStream.MUSIC),
        alarm = audioController.getVolume(MuteStream.ALARM),
    )

    fun applyMute() {
        MuteStream.entries.forEach { stream -> audioController.setVolume(stream, 0) }
        dndController.setFilter(DndMode.NONE)
    }

    fun restoreVolumes(snapshot: VolumeSnapshot) {
        audioController.setVolume(MuteStream.RING, snapshot.ring)
        audioController.setVolume(MuteStream.NOTIFICATION, snapshot.notification)
        audioController.setVolume(MuteStream.SYSTEM, snapshot.system)
        audioController.setVolume(MuteStream.MUSIC, snapshot.music)
        audioController.setVolume(MuteStream.ALARM, snapshot.alarm)
        dndController.setFilter(DndMode.ALL)
    }
}

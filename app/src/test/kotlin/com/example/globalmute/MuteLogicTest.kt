package com.example.globalmute

import com.example.globalmute.data.VolumeSnapshot
import com.example.globalmute.domain.AudioController
import com.example.globalmute.domain.DndController
import com.example.globalmute.domain.DndMode
import com.example.globalmute.domain.MuteLogic
import com.example.globalmute.domain.MuteStream
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MuteLogicTest {

    private lateinit var audioController: AudioController
    private lateinit var dndController: DndController
    private lateinit var muteLogic: MuteLogic

    @Before
    fun setUp() {
        audioController = mockk(relaxed = true)
        dndController = mockk(relaxed = true)
        muteLogic = MuteLogic(audioController, dndController)
    }

    // ─── captureVolumes ───────────────────────────────────────────────────────

    @Test
    fun `captureVolumes returns snapshot matching current stream levels`() {
        every { audioController.getVolume(MuteStream.RING) } returns 5
        every { audioController.getVolume(MuteStream.NOTIFICATION) } returns 3
        every { audioController.getVolume(MuteStream.SYSTEM) } returns 7
        every { audioController.getVolume(MuteStream.MUSIC) } returns 10
        every { audioController.getVolume(MuteStream.ALARM) } returns 6

        val snapshot = muteLogic.captureVolumes()

        assertEquals(5, snapshot.ring)
        assertEquals(3, snapshot.notification)
        assertEquals(7, snapshot.system)
        assertEquals(10, snapshot.music)
        assertEquals(6, snapshot.alarm)
    }

    @Test
    fun `captureVolumes handles all zero volumes`() {
        MuteStream.entries.forEach { stream ->
            every { audioController.getVolume(stream) } returns 0
        }

        val snapshot = muteLogic.captureVolumes()

        assertEquals(VolumeSnapshot(0, 0, 0, 0, 0), snapshot)
    }

    // ─── applyMute ────────────────────────────────────────────────────────────

    @Test
    fun `applyMute sets every stream to zero`() {
        muteLogic.applyMute()

        MuteStream.entries.forEach { stream ->
            verify { audioController.setVolume(stream, 0) }
        }
    }

    @Test
    fun `applyMute sets DND filter to NONE`() {
        muteLogic.applyMute()

        verify { dndController.setFilter(DndMode.NONE) }
    }

    @Test
    fun `applyMute calls DND filter after all volume changes`() {
        val callOrder = mutableListOf<String>()
        every { audioController.setVolume(any(), any()) } answers {
            callOrder.add("volume:${firstArg<MuteStream>()}")
        }
        every { dndController.setFilter(any()) } answers {
            callOrder.add("dnd:${firstArg<DndMode>()}")
        }

        muteLogic.applyMute()

        assertEquals("dnd:NONE", callOrder.last())
        assertTrue(callOrder.dropLast(1).all { it.startsWith("volume:") })
    }

    @Test
    fun `applyMute invokes streams and DND in exact sequence`() {
        val audio = mockk<AudioController>(relaxed = true)
        val dnd = mockk<DndController>(relaxed = true)
        val logic = MuteLogic(audio, dnd)

        logic.applyMute()

        verifySequence {
            MuteStream.entries.forEach { stream -> audio.setVolume(stream, 0) }
            dnd.setFilter(DndMode.NONE)
        }
    }

    // ─── restoreVolumes ───────────────────────────────────────────────────────

    @Test
    fun `restoreVolumes restores each stream to saved level`() {
        val snapshot = VolumeSnapshot(ring = 3, notification = 5, system = 7, music = 10, alarm = 6)

        muteLogic.restoreVolumes(snapshot)

        verify { audioController.setVolume(MuteStream.RING, 3) }
        verify { audioController.setVolume(MuteStream.NOTIFICATION, 5) }
        verify { audioController.setVolume(MuteStream.SYSTEM, 7) }
        verify { audioController.setVolume(MuteStream.MUSIC, 10) }
        verify { audioController.setVolume(MuteStream.ALARM, 6) }
    }

    @Test
    fun `restoreVolumes sets DND filter to ALL`() {
        muteLogic.restoreVolumes(VolumeSnapshot(0, 0, 0, 0, 0))

        verify { dndController.setFilter(DndMode.ALL) }
    }

    @Test
    fun `restoreVolumes can restore all-zero snapshot without error`() {
        val snapshot = VolumeSnapshot(ring = 0, notification = 0, system = 0, music = 0, alarm = 0)

        muteLogic.restoreVolumes(snapshot)

        MuteStream.entries.forEach { stream ->
            verify { audioController.setVolume(stream, 0) }
        }
        verify { dndController.setFilter(DndMode.ALL) }
    }

    // ─── round-trip ───────────────────────────────────────────────────────────

    @Test
    fun `mute then unmute round-trip restores all original volumes`() {
        every { audioController.getVolume(MuteStream.RING) } returns 4
        every { audioController.getVolume(MuteStream.NOTIFICATION) } returns 5
        every { audioController.getVolume(MuteStream.SYSTEM) } returns 6
        every { audioController.getVolume(MuteStream.MUSIC) } returns 9
        every { audioController.getVolume(MuteStream.ALARM) } returns 7

        val snapshot = muteLogic.captureVolumes()
        muteLogic.applyMute()
        muteLogic.restoreVolumes(snapshot)

        verify { audioController.setVolume(MuteStream.RING, 4) }
        verify { audioController.setVolume(MuteStream.NOTIFICATION, 5) }
        verify { audioController.setVolume(MuteStream.SYSTEM, 6) }
        verify { audioController.setVolume(MuteStream.MUSIC, 9) }
        verify { audioController.setVolume(MuteStream.ALARM, 7) }
    }
}

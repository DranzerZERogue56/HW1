package com.example.globalmute.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MuteRepository(private val context: Context) {

    val isMuted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[MuteKeys.IS_MUTED] ?: false
    }

    suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[MuteKeys.IS_MUTED] = muted
        }
    }

    suspend fun saveVolumes(snapshot: VolumeSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[MuteKeys.VOL_RING] = snapshot.ring
            prefs[MuteKeys.VOL_NOTIFICATION] = snapshot.notification
            prefs[MuteKeys.VOL_SYSTEM] = snapshot.system
            prefs[MuteKeys.VOL_MUSIC] = snapshot.music
            prefs[MuteKeys.VOL_ALARM] = snapshot.alarm
        }
    }

    suspend fun getSavedVolumes(): VolumeSnapshot? {
        return context.dataStore.data.map { prefs ->
            val ring = prefs[MuteKeys.VOL_RING] ?: return@map null
            val notification = prefs[MuteKeys.VOL_NOTIFICATION] ?: return@map null
            val system = prefs[MuteKeys.VOL_SYSTEM] ?: return@map null
            val music = prefs[MuteKeys.VOL_MUSIC] ?: return@map null
            val alarm = prefs[MuteKeys.VOL_ALARM] ?: return@map null
            VolumeSnapshot(ring, notification, system, music, alarm)
        }.first()
    }
}

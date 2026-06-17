package com.example.globalmute.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mute_prefs")

internal object MuteKeys {
    val IS_MUTED = booleanPreferencesKey("is_muted")
    val VOL_RING = intPreferencesKey("vol_ring")
    val VOL_NOTIFICATION = intPreferencesKey("vol_notification")
    val VOL_SYSTEM = intPreferencesKey("vol_system")
    val VOL_MUSIC = intPreferencesKey("vol_music")
    val VOL_ALARM = intPreferencesKey("vol_alarm")
}

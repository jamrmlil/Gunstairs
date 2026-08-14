package cz.novotny.gunstairs.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<GameSettings>
    suspend fun setSoundEnabled(enabled: Boolean)
}

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }

    override val settings: Flow<GameSettings> =
        context.gunStairsDataStore.data.map { prefs ->
            GameSettings(soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true)
        }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        context.gunStairsDataStore.edit { prefs -> prefs[Keys.SOUND_ENABLED] = enabled }
    }
}

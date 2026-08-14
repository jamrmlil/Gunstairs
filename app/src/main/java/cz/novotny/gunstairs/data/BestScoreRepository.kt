package cz.novotny.gunstairs.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Thin persistence contract so the rest of the app doesn't know DataStore exists. */
interface BestScoreRepository {
    val bestScore: Flow<Int>
    suspend fun updateIfHigher(score: Int)
    suspend fun reset()
}

class DataStoreBestScoreRepository(private val context: Context) : BestScoreRepository {
    private object Keys {
        val BEST_SCORE = intPreferencesKey("best_score")
    }

    override val bestScore: Flow<Int> =
        context.gunStairsDataStore.data.map { it[Keys.BEST_SCORE] ?: 0 }

    override suspend fun updateIfHigher(score: Int) {
        context.gunStairsDataStore.edit { prefs ->
            val current = prefs[Keys.BEST_SCORE] ?: 0
            if (score > current) {
                prefs[Keys.BEST_SCORE] = score
            }
        }
    }

    override suspend fun reset() {
        context.gunStairsDataStore.edit { prefs -> prefs[Keys.BEST_SCORE] = 0 }
    }
}

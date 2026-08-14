package cz.novotny.gunstairs.ui

import cz.novotny.gunstairs.data.BestScoreRepository
import cz.novotny.gunstairs.data.GameSettings
import cz.novotny.gunstairs.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBestScoreRepository(initial: Int = 0) : BestScoreRepository {
    private val state = MutableStateFlow(initial)
    override val bestScore: Flow<Int> = state

    override suspend fun updateIfHigher(score: Int) {
        if (score > state.value) state.value = score
    }

    override suspend fun reset() {
        state.value = 0
    }
}

class FakeSettingsRepository(initial: GameSettings = GameSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<GameSettings> = state

    override suspend fun setSoundEnabled(enabled: Boolean) {
        state.value = state.value.copy(soundEnabled = enabled)
    }
}

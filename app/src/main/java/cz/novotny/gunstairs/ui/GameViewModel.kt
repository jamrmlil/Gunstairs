package cz.novotny.gunstairs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.novotny.gunstairs.data.BestScoreRepository
import cz.novotny.gunstairs.data.SettingsRepository
import cz.novotny.gunstairs.domain.GameEngine
import cz.novotny.gunstairs.domain.GamePhase
import cz.novotny.gunstairs.domain.ShotOutcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEATH_ANIMATION_MILLIS = 450L

class GameViewModel(
    private val bestScoreRepository: BestScoreRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val engine = GameEngine()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initialBest = bestScoreRepository.bestScore.first()
            engine.setBestScore(initialBest)
            _uiState.update { it.copy(bestScore = initialBest) }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(soundEnabled = settings.soundEnabled) }
            }
        }
    }

    fun startGame() {
        engine.start()
        _uiState.update {
            it.copy(
                phase = GamePhase.PLAYING,
                stair = engine.state.stair,
                score = engine.state.score,
                barrelAngleDeg = engine.currentBarrelAngleDeg(),
                lastShotOutcome = null,
                shotSeq = 0,
                isDying = false,
            )
        }
    }

    fun restart() = startGame()

    fun returnToMenu() {
        engine.returnToMenu()
        _uiState.update { it.copy(phase = GamePhase.MENU, lastShotOutcome = null, isDying = false) }
    }

    /** Called once per rendered frame while playing; advances the barrel's swing. */
    fun onFrame(deltaMillis: Long) {
        val current = _uiState.value
        if (current.phase != GamePhase.PLAYING || current.isDying) return
        engine.advanceTime(deltaMillis)
        _uiState.update { it.copy(barrelAngleDeg = engine.currentBarrelAngleDeg()) }
    }

    fun shoot() {
        val current = _uiState.value
        if (current.phase != GamePhase.PLAYING || current.isDying) return

        val outcome = engine.shoot()
        val nextShotSeq = current.shotSeq + 1

        when (outcome) {
            ShotOutcome.HIT -> _uiState.update {
                it.copy(
                    stair = engine.state.stair,
                    score = engine.state.score,
                    barrelAngleDeg = engine.currentBarrelAngleDeg(),
                    lastShotOutcome = outcome,
                    shotSeq = nextShotSeq,
                )
            }

            ShotOutcome.MISS -> {
                _uiState.update { it.copy(lastShotOutcome = outcome, shotSeq = nextShotSeq, isDying = true) }
                viewModelScope.launch {
                    bestScoreRepository.updateIfHigher(engine.state.score)
                    delay(DEATH_ANIMATION_MILLIS)
                    _uiState.update {
                        it.copy(phase = engine.state.phase, bestScore = engine.state.bestScore, isDying = false)
                    }
                }
            }
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun resetBestScore() {
        engine.setBestScore(0)
        _uiState.update { it.copy(bestScore = 0) }
        viewModelScope.launch { bestScoreRepository.reset() }
    }
}

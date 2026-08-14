package cz.novotny.gunstairs.ui

import cz.novotny.gunstairs.domain.GameConfig
import cz.novotny.gunstairs.domain.GamePhase
import cz.novotny.gunstairs.domain.ShotOutcome

data class GameUiState(
    val phase: GamePhase = GamePhase.MENU,
    val showSettings: Boolean = false,
    val stair: Int = 0,
    val score: Int = 0,
    val bestScore: Int = 0,
    val barrelAngleDeg: Float = GameConfig.MIN_BARREL_ANGLE_DEG,
    val lastShotOutcome: ShotOutcome? = null,
    // Bumped on every shot so UI feedback (e.g. a muzzle flash) can key off it
    // and replay even when two shots in a row produce the same outcome.
    val shotSeq: Int = 0,
    // True for a brief window after a fatal miss, before phase flips to
    // GAME_OVER, so the death animation has time to play on the game screen.
    val isDying: Boolean = false,
    val soundEnabled: Boolean = true,
)

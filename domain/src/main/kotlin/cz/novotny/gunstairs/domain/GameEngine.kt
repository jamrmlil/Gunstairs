package cz.novotny.gunstairs.domain

enum class GamePhase { MENU, PLAYING, GAME_OVER }

data class GameState(
    val phase: GamePhase = GamePhase.MENU,
    val stair: Int = 0,
    val score: Int = 0,
    val bestScore: Int = 0,
)

/**
 * Drives one round of Gun Stairs: owns the barrel angle over time and resolves
 * shots against the current difficulty. Has no notion of frames or wall-clock
 * time — the caller feeds elapsed time in via [advanceTime], typically once per
 * rendered frame.
 */
class GameEngine(
    initialBestScore: Int = 0,
    private val oscillator: BarrelOscillator = BarrelOscillator(),
    private val idealAngleDeg: Float = GameConfig.IDEAL_AIM_ANGLE_DEG,
) {
    var state: GameState = GameState(bestScore = initialBestScore)
        private set

    private var elapsedInStairMillis: Long = 0L

    fun start() {
        state = GameState(phase = GamePhase.PLAYING, bestScore = state.bestScore)
        elapsedInStairMillis = 0L
    }

    fun advanceTime(deltaMillis: Long) {
        require(deltaMillis >= 0) { "deltaMillis must not be negative" }
        if (state.phase == GamePhase.PLAYING) {
            elapsedInStairMillis += deltaMillis
        }
    }

    fun currentBarrelAngleDeg(): Float {
        val period = DifficultyProgression.oscillationPeriodMillis(state.stair)
        return oscillator.angleAtDeg(elapsedInStairMillis, period)
    }

    fun shoot(): ShotOutcome {
        check(state.phase == GamePhase.PLAYING) { "Cannot shoot while phase is ${state.phase}" }
        val tolerance = DifficultyProgression.toleranceDeg(state.stair)
        val outcome = ShotResolver.resolve(currentBarrelAngleDeg(), idealAngleDeg, tolerance)
        state = when (outcome) {
            ShotOutcome.HIT -> {
                elapsedInStairMillis = 0L
                state.copy(stair = state.stair + 1, score = state.score + 1)
            }
            ShotOutcome.MISS -> state.copy(
                phase = GamePhase.GAME_OVER,
                bestScore = maxOf(state.bestScore, state.score),
            )
        }
        return outcome
    }
}

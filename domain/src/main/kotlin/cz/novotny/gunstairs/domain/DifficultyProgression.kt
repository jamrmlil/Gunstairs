package cz.novotny.gunstairs.domain

/**
 * Maps how many stairs the player has climbed to how hard the game currently is:
 * the barrel swings faster and the hit tolerance narrows, both ramping linearly
 * down to a floor over [GameConfig.DIFFICULTY_RAMP_STAIRS] stairs.
 */
object DifficultyProgression {
    fun oscillationPeriodMillis(stair: Int): Long {
        val progress = rampProgress(stair)
        val range = GameConfig.BASE_OSCILLATION_PERIOD_MILLIS - GameConfig.MIN_OSCILLATION_PERIOD_MILLIS
        val period = GameConfig.BASE_OSCILLATION_PERIOD_MILLIS - (range * progress).toLong()
        return period.coerceAtLeast(GameConfig.MIN_OSCILLATION_PERIOD_MILLIS)
    }

    fun toleranceDeg(stair: Int): Float {
        val progress = rampProgress(stair)
        val range = GameConfig.BASE_TOLERANCE_DEG - GameConfig.MIN_TOLERANCE_DEG
        val tolerance = GameConfig.BASE_TOLERANCE_DEG - range * progress
        return tolerance.coerceAtLeast(GameConfig.MIN_TOLERANCE_DEG)
    }

    private fun rampProgress(stair: Int): Float =
        (stair.coerceAtLeast(0).toFloat() / GameConfig.DIFFICULTY_RAMP_STAIRS).coerceIn(0f, 1f)
}

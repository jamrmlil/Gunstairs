package cz.novotny.gunstairs.domain

/** Computes the gun barrel's angle as it sweeps back and forth over time. */
class BarrelOscillator(
    private val minAngleDeg: Float = GameConfig.MIN_BARREL_ANGLE_DEG,
    private val maxAngleDeg: Float = GameConfig.MAX_BARREL_ANGLE_DEG,
) {
    init {
        require(maxAngleDeg > minAngleDeg) { "maxAngleDeg must be greater than minAngleDeg" }
    }

    /**
     * Angle at [elapsedMillis] into a repeating min -> max -> min sweep of length
     * [periodMillis]. A triangle wave (linear per half-period) is used instead of a
     * sine so the barrel moves at constant angular speed, matching a classic
     * "stop the moving needle" feel rather than easing near the edges.
     */
    fun angleAtDeg(elapsedMillis: Long, periodMillis: Long): Float {
        require(periodMillis > 0) { "periodMillis must be positive" }
        val range = maxAngleDeg - minAngleDeg
        val phase = (elapsedMillis % periodMillis).toFloat() / periodMillis
        val triangle = if (phase < 0.5f) phase * 2f else 2f - phase * 2f
        return minAngleDeg + triangle * range
    }
}

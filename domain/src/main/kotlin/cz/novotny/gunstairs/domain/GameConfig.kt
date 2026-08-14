package cz.novotny.gunstairs.domain

/** Tunable gameplay constants. Pure data — no Android dependency. */
object GameConfig {
    const val MIN_BARREL_ANGLE_DEG = -10f
    const val MAX_BARREL_ANGLE_DEG = 80f

    // Angle the barrel must be at to be aimed straight at the enemy one stair up.
    // Fixed because the player/enemy stair geometry is identical on every stair.
    const val IDEAL_AIM_ANGLE_DEG = 50f

    const val BASE_OSCILLATION_PERIOD_MILLIS = 2600L
    const val MIN_OSCILLATION_PERIOD_MILLIS = 900L

    const val BASE_TOLERANCE_DEG = 9f
    const val MIN_TOLERANCE_DEG = 2.5f

    // Difficulty reaches its floor (fastest swing, tightest tolerance) around this stair.
    const val DIFFICULTY_RAMP_STAIRS = 18
}

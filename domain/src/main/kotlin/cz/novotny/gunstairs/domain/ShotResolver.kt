package cz.novotny.gunstairs.domain

import kotlin.math.abs

enum class ShotOutcome { HIT, MISS }

/** Decides whether a shot fired at a given barrel angle hits the enemy. */
object ShotResolver {
    fun resolve(barrelAngleDeg: Float, idealAngleDeg: Float, toleranceDeg: Float): ShotOutcome {
        val diff = abs(barrelAngleDeg - idealAngleDeg)
        return if (diff <= toleranceDeg) ShotOutcome.HIT else ShotOutcome.MISS
    }
}

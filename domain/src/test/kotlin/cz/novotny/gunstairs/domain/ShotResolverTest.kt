package cz.novotny.gunstairs.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShotResolverTest {

    @Test
    fun `exact ideal angle is a hit`() {
        assertEquals(ShotOutcome.HIT, ShotResolver.resolve(50f, idealAngleDeg = 50f, toleranceDeg = 5f))
    }

    @Test
    fun `angle within tolerance above ideal is a hit`() {
        assertEquals(ShotOutcome.HIT, ShotResolver.resolve(54f, idealAngleDeg = 50f, toleranceDeg = 5f))
    }

    @Test
    fun `angle within tolerance below ideal is a hit`() {
        assertEquals(ShotOutcome.HIT, ShotResolver.resolve(46f, idealAngleDeg = 50f, toleranceDeg = 5f))
    }

    @Test
    fun `angle exactly at the tolerance boundary is a hit`() {
        assertEquals(ShotOutcome.HIT, ShotResolver.resolve(55f, idealAngleDeg = 50f, toleranceDeg = 5f))
    }

    @Test
    fun `angle just outside tolerance is a miss`() {
        assertEquals(ShotOutcome.MISS, ShotResolver.resolve(55.1f, idealAngleDeg = 50f, toleranceDeg = 5f))
    }

    @Test
    fun `angle far below ideal is a miss`() {
        assertEquals(ShotOutcome.MISS, ShotResolver.resolve(-10f, idealAngleDeg = 50f, toleranceDeg = 5f))
    }
}

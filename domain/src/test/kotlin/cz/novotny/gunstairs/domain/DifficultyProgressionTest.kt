package cz.novotny.gunstairs.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyProgressionTest {

    @Test
    fun `stair zero uses base oscillation period`() {
        assertEquals(
            GameConfig.BASE_OSCILLATION_PERIOD_MILLIS,
            DifficultyProgression.oscillationPeriodMillis(0),
        )
    }

    @Test
    fun `stair zero uses base tolerance`() {
        assertEquals(GameConfig.BASE_TOLERANCE_DEG, DifficultyProgression.toleranceDeg(0), 0.001f)
    }

    @Test
    fun `oscillation period strictly decreases as stairs climb within the ramp`() {
        val period5 = DifficultyProgression.oscillationPeriodMillis(5)
        val period10 = DifficultyProgression.oscillationPeriodMillis(10)
        assertTrue(period10 < period5)
    }

    @Test
    fun `tolerance strictly decreases as stairs climb within the ramp`() {
        val tolerance5 = DifficultyProgression.toleranceDeg(5)
        val tolerance10 = DifficultyProgression.toleranceDeg(10)
        assertTrue(tolerance10 < tolerance5)
    }

    @Test
    fun `oscillation period floors at the minimum beyond the ramp`() {
        assertEquals(
            GameConfig.MIN_OSCILLATION_PERIOD_MILLIS,
            DifficultyProgression.oscillationPeriodMillis(GameConfig.DIFFICULTY_RAMP_STAIRS + 50),
        )
    }

    @Test
    fun `tolerance floors at the minimum beyond the ramp`() {
        assertEquals(
            GameConfig.MIN_TOLERANCE_DEG,
            DifficultyProgression.toleranceDeg(GameConfig.DIFFICULTY_RAMP_STAIRS + 50),
            0.001f,
        )
    }

    @Test
    fun `negative stair is treated like stair zero`() {
        assertEquals(
            DifficultyProgression.oscillationPeriodMillis(0),
            DifficultyProgression.oscillationPeriodMillis(-3),
        )
        assertEquals(
            DifficultyProgression.toleranceDeg(0),
            DifficultyProgression.toleranceDeg(-3),
            0.001f,
        )
    }
}

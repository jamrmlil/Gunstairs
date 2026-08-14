package cz.novotny.gunstairs.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BarrelOscillatorTest {

    private val oscillator = BarrelOscillator(minAngleDeg = 0f, maxAngleDeg = 100f)

    @Test
    fun `angle at start of period is minimum`() {
        assertEquals(0f, oscillator.angleAtDeg(elapsedMillis = 0, periodMillis = 1000), 0.001f)
    }

    @Test
    fun `angle at half period is maximum`() {
        assertEquals(100f, oscillator.angleAtDeg(elapsedMillis = 500, periodMillis = 1000), 0.001f)
    }

    @Test
    fun `angle at full period wraps back to minimum`() {
        assertEquals(0f, oscillator.angleAtDeg(elapsedMillis = 1000, periodMillis = 1000), 0.001f)
    }

    @Test
    fun `angle at quarter period is halfway up the range`() {
        assertEquals(50f, oscillator.angleAtDeg(elapsedMillis = 250, periodMillis = 1000), 0.001f)
    }

    @Test
    fun `angle at three quarter period is halfway back down`() {
        assertEquals(50f, oscillator.angleAtDeg(elapsedMillis = 750, periodMillis = 1000), 0.001f)
    }

    @Test
    fun `angle is symmetric around the peak`() {
        val beforePeak = oscillator.angleAtDeg(elapsedMillis = 400, periodMillis = 1000)
        val afterPeak = oscillator.angleAtDeg(elapsedMillis = 600, periodMillis = 1000)
        assertEquals(beforePeak, afterPeak, 0.001f)
    }

    @Test
    fun `second cycle repeats the first`() {
        val first = oscillator.angleAtDeg(elapsedMillis = 300, periodMillis = 1000)
        val second = oscillator.angleAtDeg(elapsedMillis = 1300, periodMillis = 1000)
        assertEquals(first, second, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero period is rejected`() {
        oscillator.angleAtDeg(elapsedMillis = 0, periodMillis = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `max angle must be greater than min angle`() {
        BarrelOscillator(minAngleDeg = 10f, maxAngleDeg = 10f)
    }
}

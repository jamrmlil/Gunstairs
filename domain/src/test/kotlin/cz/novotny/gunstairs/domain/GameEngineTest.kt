package cz.novotny.gunstairs.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GameEngineTest {

    // Ideal angle pinned to the oscillator's start so shots at t=0 are deterministic hits.
    private fun newEngine(initialBestScore: Int = 0) = GameEngine(
        initialBestScore = initialBestScore,
        oscillator = BarrelOscillator(minAngleDeg = 0f, maxAngleDeg = 90f),
        idealAngleDeg = 0f,
    )

    @Test
    fun `initial state is menu with persisted best score`() {
        val engine = newEngine(initialBestScore = 7)
        assertEquals(GamePhase.MENU, engine.state.phase)
        assertEquals(7, engine.state.bestScore)
        assertEquals(0, engine.state.score)
    }

    @Test
    fun `start resets stair and score but keeps best score`() {
        val engine = newEngine(initialBestScore = 7)
        engine.start()
        assertEquals(GamePhase.PLAYING, engine.state.phase)
        assertEquals(0, engine.state.stair)
        assertEquals(0, engine.state.score)
        assertEquals(7, engine.state.bestScore)
    }

    @Test
    fun `shooting on target hits, advances stair and score, resets barrel timer`() {
        val engine = newEngine()
        engine.start()

        assertEquals(ShotOutcome.HIT, engine.shoot())
        assertEquals(1, engine.state.stair)
        assertEquals(1, engine.state.score)
        assertEquals(GamePhase.PLAYING, engine.state.phase)
        // Timer reset by the hit means the barrel is back at its starting angle.
        assertEquals(0f, engine.currentBarrelAngleDeg(), 0.001f)
    }

    @Test
    fun `shooting off target misses and ends the game`() {
        val engine = newEngine()
        engine.start()
        engine.advanceTime(DifficultyProgression.oscillationPeriodMillis(0) / 2)

        assertEquals(ShotOutcome.MISS, engine.shoot())
        assertEquals(GamePhase.GAME_OVER, engine.state.phase)
        assertEquals(0, engine.state.score)
    }

    @Test
    fun `game over keeps a higher previous best score untouched`() {
        val engine = newEngine(initialBestScore = 5)
        engine.start()
        engine.advanceTime(DifficultyProgression.oscillationPeriodMillis(0) / 2)
        engine.shoot()

        assertEquals(5, engine.state.bestScore)
    }

    @Test
    fun `game over raises best score when the run beat the previous best`() {
        val engine = newEngine(initialBestScore = 0)
        engine.start()
        engine.shoot() // hit -> score 1, stair 1
        engine.advanceTime(DifficultyProgression.oscillationPeriodMillis(1) / 2)
        engine.shoot() // miss -> game over

        assertEquals(1, engine.state.score)
        assertEquals(1, engine.state.bestScore)
    }

    @Test(expected = IllegalStateException::class)
    fun `shooting before start is rejected`() {
        newEngine().shoot()
    }

    @Test(expected = IllegalStateException::class)
    fun `shooting after game over is rejected`() {
        val engine = newEngine()
        engine.start()
        engine.advanceTime(DifficultyProgression.oscillationPeriodMillis(0) / 2)
        engine.shoot() // miss -> game over
        engine.shoot()
    }

    @Test
    fun `advanceTime is a no-op outside the playing phase`() {
        val engine = newEngine()
        engine.advanceTime(500)
        assertEquals(0f, engine.currentBarrelAngleDeg(), 0.001f)
    }

    @Test
    fun `returnToMenu resets the run but keeps best score`() {
        val engine = newEngine(initialBestScore = 4)
        engine.start()
        engine.shoot()

        engine.returnToMenu()

        assertEquals(GamePhase.MENU, engine.state.phase)
        assertEquals(0, engine.state.stair)
        assertEquals(0, engine.state.score)
        assertEquals(4, engine.state.bestScore)
    }

    @Test
    fun `setBestScore overwrites the known best score`() {
        val engine = newEngine(initialBestScore = 0)
        engine.setBestScore(12)
        assertEquals(12, engine.state.bestScore)
    }
}

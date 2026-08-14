package cz.novotny.gunstairs.ui

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cz.novotny.gunstairs.domain.DifficultyProgression
import cz.novotny.gunstairs.domain.GameConfig
import cz.novotny.gunstairs.ui.theme.GunStairsTheme
import java.time.Duration
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Exercises the critical path end to end through the real Compose UI:
 * Start -> shoot (hit) -> shoot (miss) -> Game Over -> Restart.
 *
 * Runs as a JVM unit test via Robolectric (./gradlew test) since CI has no
 * Android emulator for instrumented androidTest.
 *
 * The game screen runs a perpetual per-frame loop (the barrel's swing), so
 * the test clock is kept off auto-advance — otherwise Compose's idle wait
 * never finishes, since there's always another frame pending. Instead, each
 * step that needs a fresh layout/draw pass to become visible is advanced by
 * exactly one frame via [advanceOneFrame]. The barrel's angle is a pure
 * function of elapsed time, so hit/miss timing is driven deterministically
 * through [cz.novotny.gunstairs.ui.GameViewModel.onFrame] (the same entry
 * point the real loop uses) rather than by racing the frame clock.
 */
// Pinned to API 28: newer Compose (1.7+) has a known Robolectric rendering/
// event regression on API 27 and 29-34 (see robolectric/robolectric#9595).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GunStairsAppCriticalPathTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        viewModel = GameViewModel(
            bestScoreRepository = FakeBestScoreRepository(),
            settingsRepository = FakeSettingsRepository(),
        )
        composeTestRule.setContent {
            GunStairsTheme { GunStairsApp(viewModel) }
        }
        composeTestRule.mainClock.autoAdvance = false
        advanceOneFrame() // let the initial menu composition measure/layout/draw
    }

    @Test
    fun startShootHitShootMissGameOverThenRestart() {
        composeTestRule.onNodeWithText("Start").performClick()
        advanceOneFrame() // mount the game screen (its frame loop's first tick is a no-op timing-wise)

        composeTestRule.onNodeWithTag("game_surface").assertIsDisplayed()

        // Position the barrel at exactly the ideal aim angle for a guaranteed hit.
        // This is a direct call, not a frame-clock tick, so it doesn't disturb
        // the "first tick is a no-op" accounting above.
        viewModel.onFrame(millisToIdealAngle(stair = 0))
        composeTestRule.onNodeWithTag("game_surface").performClick()
        advanceOneFrame() // render the post-hit score

        composeTestRule.onNodeWithText("1").assertIsDisplayed() // score after the hit

        // The barrel resets to its minimum angle on every hit, far outside
        // tolerance of the ideal aim angle, so an immediate second shot misses.
        composeTestRule.onNodeWithTag("game_surface").performClick()

        // The miss keeps the game screen up briefly for the death animation
        // before flipping to Game Over; advance real (Robolectric) time so
        // that delay-based transition fires. This is independent of the
        // paused Compose frame clock above.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_000))
        advanceOneFrame() // render the game-over screen

        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
        composeTestRule.onNodeWithText("Score: 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Best: 1").assertIsDisplayed()

        composeTestRule.onNodeWithText("Restart").performClick()
        advanceOneFrame() // mount the fresh game screen for the new run

        composeTestRule.onNodeWithTag("game_surface").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertIsDisplayed() // score reset for the new run
    }

    /** Renders exactly one frame's worth of recomposition/measure/layout/draw. */
    private fun advanceOneFrame() {
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    private fun millisToIdealAngle(stair: Int): Long {
        val period = DifficultyProgression.oscillationPeriodMillis(stair)
        val range = GameConfig.MAX_BARREL_ANGLE_DEG - GameConfig.MIN_BARREL_ANGLE_DEG
        val fraction = (GameConfig.IDEAL_AIM_ANGLE_DEG - GameConfig.MIN_BARREL_ANGLE_DEG) / range
        return (fraction * (period / 2f)).toLong()
    }
}

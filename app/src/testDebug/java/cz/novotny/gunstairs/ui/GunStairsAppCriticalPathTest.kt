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
 * The barrel's angle is a pure function of elapsed time (see BarrelOscillator),
 * so instead of racing the real Compose frame clock, the test drives it
 * directly via GameViewModel.onFrame(...) — the same entry point the real
 * frame loop uses — to land on a deterministic hit, then relies on the
 * barrel resetting near its start angle (far from the ideal aim angle) for
 * a deterministic miss on the very next shot.
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
    }

    @Test
    fun startShootHitShootMissGameOverThenRestart() {
        composeTestRule.onNodeWithText("Start").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("game_surface").assertIsDisplayed()

        // Advance the barrel to exactly the ideal aim angle for a guaranteed hit.
        composeTestRule.runOnIdle { viewModel.onFrame(millisToIdealAngle(stair = 0)) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("game_surface").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("1").assertIsDisplayed() // score after the hit

        // The barrel resets to its minimum angle on every hit, far outside
        // tolerance of the ideal aim angle, so an immediate second shot misses.
        composeTestRule.onNodeWithTag("game_surface").performClick()

        // The miss keeps the game screen up briefly for the death animation
        // before flipping to Game Over; advance real (Robolectric) time so
        // that delay-based transition fires.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_000))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Game Over").assertIsDisplayed()
        composeTestRule.onNodeWithText("Score: 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Best: 1").assertIsDisplayed()

        composeTestRule.onNodeWithText("Restart").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("game_surface").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertIsDisplayed() // score reset for the new run
    }

    private fun millisToIdealAngle(stair: Int): Long {
        val period = DifficultyProgression.oscillationPeriodMillis(stair)
        val range = GameConfig.MAX_BARREL_ANGLE_DEG - GameConfig.MIN_BARREL_ANGLE_DEG
        val fraction = (GameConfig.IDEAL_AIM_ANGLE_DEG - GameConfig.MIN_BARREL_ANGLE_DEG) / range
        return (fraction * (period / 2f)).toLong()
    }
}

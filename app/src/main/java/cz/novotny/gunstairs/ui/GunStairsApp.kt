package cz.novotny.gunstairs.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cz.novotny.gunstairs.domain.GamePhase

@Composable
fun GunStairsApp(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            uiState.showSettings -> SettingsScreen(
                soundEnabled = uiState.soundEnabled,
                onSoundEnabledChange = viewModel::setSoundEnabled,
                onResetBestScore = viewModel::resetBestScore,
                onBack = viewModel::closeSettings,
            )

            uiState.phase == GamePhase.PLAYING -> GameScreen(
                uiState = uiState,
                onShoot = viewModel::shoot,
                onFrame = viewModel::onFrame,
            )

            uiState.phase == GamePhase.GAME_OVER -> GameOverScreen(
                score = uiState.score,
                bestScore = uiState.bestScore,
                onRestart = viewModel::restart,
                onMenu = viewModel::returnToMenu,
            )

            else -> MenuScreen(
                bestScore = uiState.bestScore,
                onStart = viewModel::startGame,
                onOpenSettings = viewModel::openSettings,
            )
        }
    }
}

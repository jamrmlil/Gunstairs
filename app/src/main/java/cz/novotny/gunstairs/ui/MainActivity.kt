package cz.novotny.gunstairs.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.novotny.gunstairs.data.DataStoreBestScoreRepository
import cz.novotny.gunstairs.data.DataStoreSettingsRepository
import cz.novotny.gunstairs.ui.theme.GunStairsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GunStairsTheme {
                val appContext = applicationContext
                val viewModel: GameViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            GameViewModel(
                                bestScoreRepository = DataStoreBestScoreRepository(appContext),
                                settingsRepository = DataStoreSettingsRepository(appContext),
                            )
                        }
                    },
                )
                GunStairsApp(viewModel)
            }
        }
    }
}

package cz.novotny.gunstairs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.novotny.gunstairs.R

@Composable
fun GameOverScreen(
    score: Int,
    bestScore: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.gameover_title),
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.gameover_score, score), fontSize = 20.sp)
            Text(text = stringResource(R.string.gameover_best, bestScore), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRestart) {
                Text(text = stringResource(R.string.gameover_restart))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onMenu) {
                Text(text = stringResource(R.string.gameover_menu))
            }
        }
    }
}

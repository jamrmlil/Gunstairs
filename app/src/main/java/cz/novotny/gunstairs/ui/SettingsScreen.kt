package cz.novotny.gunstairs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.novotny.gunstairs.R

@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onResetBestScore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backDescription = stringResource(R.string.settings_back_cd)
    val soundToggleDescription = stringResource(R.string.settings_sound_cd)
    val resetDescription = stringResource(R.string.settings_reset_best_cd)

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backDescription }) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
            }
            Text(text = stringResource(R.string.settings_title), fontSize = 24.sp, modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.settings_sound))
            Switch(
                checked = soundEnabled,
                onCheckedChange = onSoundEnabledChange,
                modifier = Modifier.semantics { contentDescription = soundToggleDescription },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onResetBestScore,
            modifier = Modifier.semantics { contentDescription = resetDescription },
        ) {
            Text(text = stringResource(R.string.settings_reset_best))
        }
    }
}

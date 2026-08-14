package cz.novotny.gunstairs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Gun Stairs is dark-only by design: a night shooting-range staircase reads
// better than a light variant, and a small arcade game doesn't need effort
// split across two palettes. See README for the full rationale.
private val GunStairsColorScheme = darkColorScheme(
    primary = Color(0xFF3A7CA5),
    secondary = Color(0xFFF2A65A),
    tertiary = Color(0xFFE84855),
    background = Color(0xFF0F1115),
    surface = Color(0xFF15181E),
    error = Color(0xFFE84855),
)

@Composable
fun GunStairsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GunStairsColorScheme,
        content = content,
    )
}

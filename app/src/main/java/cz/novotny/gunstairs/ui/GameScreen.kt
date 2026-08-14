package cz.novotny.gunstairs.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.novotny.gunstairs.R
import cz.novotny.gunstairs.domain.GamePhase
import cz.novotny.gunstairs.domain.ShotOutcome
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.isActive

@Composable
fun GameScreen(
    uiState: GameUiState,
    onShoot: () -> Unit,
    onFrame: (deltaMillis: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(uiState.phase) {
        if (uiState.phase != GamePhase.PLAYING) return@LaunchedEffect
        var lastFrameNanos = -1L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos >= 0) {
                    val deltaMillis = (frameNanos - lastFrameNanos) / 1_000_000
                    onFrame(deltaMillis)
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(uiState.shotSeq) {
        if (uiState.shotSeq == 0) return@LaunchedEffect
        flashAlpha.snapTo(1f)
        flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 300))
    }

    val shootDescription = stringResource(R.string.game_surface_cd)
    val scoreDescription = stringResource(R.string.game_score_cd, uiState.score)

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = shootDescription }
            .testTag("game_surface")
            .pointerInput(uiState.phase, uiState.isDying) {
                detectTapGestures(onTap = { onShoot() })
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGameScene(
                barrelAngleDeg = uiState.barrelAngleDeg,
                lastShotOutcome = uiState.lastShotOutcome,
                isDying = uiState.isDying,
                flashAlpha = flashAlpha.value,
            )
        }

        Text(
            text = "${uiState.score}",
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .semantics { contentDescription = scoreDescription },
        )

        if (uiState.isDying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE84855).copy(alpha = 0.25f)),
            )
        }
    }
}

private fun DrawScope.drawGameScene(
    barrelAngleDeg: Float,
    lastShotOutcome: ShotOutcome?,
    isDying: Boolean,
    flashAlpha: Float,
) {
    val stepColor = Color(0xFF23262E)
    val stepHighlight = Color(0xFF2E3340)
    val playerColor = Color(0xFF3A7CA5)
    val enemyColor = if (isDying) Color(0xFFE84855) else Color(0xFFB0463B)
    val muzzleColor = Color(0xFFF2A65A)

    val w = size.width
    val h = size.height

    // Three ascending stairs: player's current stair (bottom), the enemy's
    // stair one level up, and a hint of the stair above that.
    val stepHeight = h * 0.16f
    val stepDepth = w * 0.55f

    val playerStepTop = h * 0.78f
    val enemyStepTop = playerStepTop - stepHeight
    val topStepTop = enemyStepTop - stepHeight

    drawRect(stepColor, topLeft = Offset(0f, topStepTop), size = Size(stepDepth * 0.55f, h - topStepTop))
    drawRect(stepHighlight, topLeft = Offset(0f, enemyStepTop), size = Size(stepDepth * 0.8f, h - enemyStepTop))
    drawRect(stepColor, topLeft = Offset(0f, playerStepTop), size = Size(w, h - playerStepTop))

    // Enemy figure, one stair up.
    val enemyX = stepDepth * 0.65f
    drawStickFigure(enemyX, feetY = enemyStepTop, bodyHeight = h * 0.09f, color = enemyColor, isFalling = isDying)

    // Player figure and rotating gun barrel, bottom stair.
    val playerX = w * 0.28f
    val playerFeetY = playerStepTop
    val shoulderY = playerFeetY - h * 0.09f
    drawStickFigure(playerX, feetY = playerFeetY, bodyHeight = h * 0.09f, color = playerColor, isFalling = false)

    val barrelLength = w * 0.22f
    rotate(degrees = -barrelAngleDeg, pivot = Offset(playerX, shoulderY)) {
        drawLine(
            color = playerColor,
            start = Offset(playerX, shoulderY),
            end = Offset(playerX + barrelLength, shoulderY),
            strokeWidth = 6f,
        )
    }

    if (flashAlpha > 0f) {
        val angleRad = Math.toRadians(barrelAngleDeg.toDouble())
        val tipX = playerX + barrelLength * cos(angleRad).toFloat()
        val tipY = shoulderY - barrelLength * sin(angleRad).toFloat()
        val flashColor = if (lastShotOutcome == ShotOutcome.HIT) muzzleColor else Color(0xFFE84855)
        drawCircle(
            color = flashColor.copy(alpha = flashAlpha),
            radius = 18f + 10f * flashAlpha,
            center = Offset(tipX, tipY),
        )
    }
}

private fun DrawScope.drawStickFigure(
    x: Float,
    feetY: Float,
    bodyHeight: Float,
    color: Color,
    isFalling: Boolean,
) {
    val headRadius = bodyHeight * 0.35f
    // A falling figure's body collapses toward the step instead of standing tall.
    val effectiveFeetY = if (isFalling) feetY - bodyHeight * 0.4f else feetY
    val shoulderY = effectiveFeetY - bodyHeight + (if (isFalling) bodyHeight * 0.6f else 0f)

    drawCircle(color, radius = headRadius, center = Offset(x, shoulderY - headRadius * 1.6f))
    drawLine(color, Offset(x, shoulderY - headRadius * 0.3f), Offset(x, effectiveFeetY), strokeWidth = 8f)
    drawLine(color, Offset(x, effectiveFeetY), Offset(x - headRadius, effectiveFeetY + headRadius * 0.6f), strokeWidth = 8f)
    drawLine(color, Offset(x, effectiveFeetY), Offset(x + headRadius, effectiveFeetY + headRadius * 0.6f), strokeWidth = 8f)
}

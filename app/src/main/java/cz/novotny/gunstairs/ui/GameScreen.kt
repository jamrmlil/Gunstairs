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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.novotny.gunstairs.R
import cz.novotny.gunstairs.domain.GameConfig
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

// The enemy's on-screen position is derived directly from the domain's ideal
// aim angle (rather than a separately hand-picked layout), so that "the
// barrel visually points at the enemy" and "the shot is a hit" are always
// the same thing. Placing the two independently previously meant the enemy
// rendered almost on top of the player while the angle that actually
// counted as a hit pointed somewhere else entirely — reliably unplayable.
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

    val legLength = h * 0.095f
    val playerPivot = Offset(w * 0.26f, h * 0.68f)

    val idealAngleRad = Math.toRadians(GameConfig.IDEAL_AIM_ANGLE_DEG.toDouble())
    val aimDistance = w * 0.78f
    val enemyPivot = Offset(
        x = playerPivot.x + (aimDistance * cos(idealAngleRad)).toFloat(),
        y = playerPivot.y - (aimDistance * sin(idealAngleRad)).toFloat(),
    )

    val playerFeetY = playerPivot.y + legLength * 1.85f
    val enemyFeetY = enemyPivot.y + legLength * 1.85f
    val enemyStepLeft = (enemyPivot.x - w * 0.24f).coerceAtLeast(0f)
    val topStepLeft = (enemyPivot.x - w * 0.42f).coerceAtLeast(0f)
    val topStepTop = enemyFeetY - (playerFeetY - enemyFeetY)

    // A hint of the stair above the enemy, purely for depth.
    drawRect(stepColor, topLeft = Offset(topStepLeft, topStepTop), size = Size(w - topStepLeft, enemyFeetY - topStepTop))
    // The enemy's stair.
    drawRect(stepHighlight, topLeft = Offset(enemyStepLeft, enemyFeetY), size = Size(w - enemyStepLeft, playerFeetY - enemyFeetY))
    // The player's stair.
    drawRect(stepColor, topLeft = Offset(0f, playerFeetY), size = Size(w, h - playerFeetY))

    // A faint always-visible target ring so the player has something concrete
    // to aim the barrel at, rather than guessing where "on target" is.
    drawCircle(
        color = muzzleColor.copy(alpha = 0.22f),
        radius = legLength * 1.1f,
        center = enemyPivot,
        style = Stroke(width = 3f),
    )

    drawCharacter(playerPivot, legLength, playerColor, isEnemy = false, collapseFraction = 0f)
    drawCharacter(enemyPivot, legLength, enemyColor, isEnemy = true, collapseFraction = if (isDying) 1f else 0f)

    // Matches aimDistance exactly so a perfectly aimed shot's muzzle flash
    // lands right on the target ring, not short of or past it.
    val barrelLength = aimDistance
    rotate(degrees = -barrelAngleDeg, pivot = playerPivot) {
        drawLine(
            color = playerColor,
            start = playerPivot,
            end = Offset(playerPivot.x + barrelLength, playerPivot.y),
            strokeWidth = 7f,
            cap = StrokeCap.Round,
        )
    }

    if (flashAlpha > 0f) {
        val angleRad = Math.toRadians(barrelAngleDeg.toDouble())
        val tipX = playerPivot.x + barrelLength * cos(angleRad).toFloat()
        val tipY = playerPivot.y - barrelLength * sin(angleRad).toFloat()
        val flashColor = if (lastShotOutcome == ShotOutcome.HIT) muzzleColor else Color(0xFFE84855)
        drawCircle(
            color = flashColor.copy(alpha = flashAlpha),
            radius = 18f + 10f * flashAlpha,
            center = Offset(tipX, tipY),
        )
    }
}

/** A small, moderately detailed stylized figure: hat, head, torso, arms, legs. */
private fun DrawScope.drawCharacter(
    pivot: Offset,
    legLength: Float,
    color: Color,
    isEnemy: Boolean,
    collapseFraction: Float,
) {
    val headRadius = legLength * 0.5f
    val torsoWidth = legLength * 0.62f
    val torsoHeight = legLength * 0.85f
    val hip = Offset(pivot.x, pivot.y + torsoHeight)
    val feet = Offset(pivot.x, hip.y + legLength)

    // A hit enemy topples sideways around its feet instead of just fading out.
    rotate(degrees = 80f * collapseFraction, pivot = feet) {
        drawLine(color, hip, Offset(feet.x - legLength * 0.28f, feet.y), strokeWidth = 9f, cap = StrokeCap.Round)
        drawLine(color, hip, Offset(feet.x + legLength * 0.28f, feet.y), strokeWidth = 9f, cap = StrokeCap.Round)

        drawRoundRect(
            color = color,
            topLeft = Offset(pivot.x - torsoWidth / 2f, pivot.y),
            size = Size(torsoWidth, torsoHeight),
            cornerRadius = CornerRadius(torsoWidth * 0.35f),
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.25f),
            start = Offset(pivot.x - torsoWidth / 2f, hip.y - torsoHeight * 0.12f),
            end = Offset(pivot.x + torsoWidth / 2f, hip.y - torsoHeight * 0.12f),
            strokeWidth = 3f,
        )

        val headCenter = Offset(pivot.x, pivot.y - headRadius * 1.15f)
        drawCircle(color, radius = headRadius, center = headCenter)
        drawLine(
            color = color,
            start = Offset(headCenter.x - headRadius * 1.3f, headCenter.y - headRadius * 0.15f),
            end = Offset(headCenter.x + headRadius * 1.3f, headCenter.y - headRadius * 0.15f),
            strokeWidth = 5f,
            cap = StrokeCap.Round,
        )

        if (isEnemy) {
            // Both arms angled toward the player, holding a small pistol.
            val elbow = Offset(pivot.x - torsoWidth * 0.4f, pivot.y + torsoHeight * 0.25f)
            val gunHand = Offset(pivot.x - torsoWidth * 0.95f, hip.y - torsoHeight * 0.05f)
            drawLine(color, Offset(pivot.x - torsoWidth * 0.3f, pivot.y + torsoHeight * 0.1f), elbow, strokeWidth = 7f, cap = StrokeCap.Round)
            drawLine(color, elbow, gunHand, strokeWidth = 7f, cap = StrokeCap.Round)
            drawLine(color, gunHand, Offset(gunHand.x - legLength * 0.32f, gunHand.y - legLength * 0.05f), strokeWidth = 8f, cap = StrokeCap.Round)
        } else {
            // Off-hand only; the rotating gun/arm is drawn separately by the caller.
            drawLine(
                color = color,
                start = Offset(pivot.x + torsoWidth * 0.4f, pivot.y + torsoHeight * 0.2f),
                end = Offset(pivot.x + torsoWidth * 0.5f, hip.y),
                strokeWidth = 7f,
                cap = StrokeCap.Round,
            )
        }
    }
}

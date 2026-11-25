package com.davidguijo.ferro.core.designsystem.theme

import android.provider.Settings
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

/**
 * Tokens de movimiento y háptica de Ferro según docs/design/README.md:
 * - 120 ms para pulsaciones y casillas
 * - 200 ms para cambios de estado
 * - 300 ms para hojas y modales
 * - Curva de salida rápida (ease-out), sin rebotes
 * - Si el sistema pide reducir animaciones, solo fundidos (sin desplazamientos ni escalados)
 */
object FerroMotion {
    const val DURATION_FAST_MS = 120
    const val DURATION_STATE_MS = 200
    const val DURATION_MODAL_MS = 300

    val EaseOut: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    fun <T> fastSpec(): TweenSpec<T> = tween(durationMillis = DURATION_FAST_MS, easing = EaseOut)

    fun <T> stateSpec(): TweenSpec<T> = tween(durationMillis = DURATION_STATE_MS, easing = EaseOut)

    fun <T> modalSpec(): TweenSpec<T> = tween(durationMillis = DURATION_MODAL_MS, easing = EaseOut)
}

@Immutable
data class FerroReduceMotionFlag(val reduceMotion: Boolean)

val LocalFerroReduceMotion = staticCompositionLocalOf { FerroReduceMotionFlag(false) }

/** Lee `ANIMATOR_DURATION_SCALE` del sistema para detectar si el usuario pidió reducir animaciones. */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/** Háptica coherente de Ferro para marcar series, récords, fin de descanso y borrados. */
object FerroHaptics {
    fun tick(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun confirm(view: View, haptic: HapticFeedback) {
        val performed = ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
        if (!performed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun delete(view: View, haptic: HapticFeedback) {
        val performed = ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.REJECT)
        if (!performed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

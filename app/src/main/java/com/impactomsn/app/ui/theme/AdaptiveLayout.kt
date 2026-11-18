package com.impactomsn.app.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Layout mode follows the current window width, including a resized Mac window. */
enum class AdaptiveWindowMode {
    Mobile,
    Tablet,
    Desktop,
}

val LocalAdaptiveWindowMode = staticCompositionLocalOf { AdaptiveWindowMode.Mobile }

private fun adaptiveWindowMode(width: Dp): AdaptiveWindowMode = when {
    width < 600.dp -> AdaptiveWindowMode.Mobile
    width < 840.dp -> AdaptiveWindowMode.Tablet
    else -> AdaptiveWindowMode.Desktop
}

/**
 * Provides a responsive mode to every screen. This is intentionally based on
 * constraints rather than device type, so rotation and desktop resizing update
 * the UI immediately.
 */
@Composable
fun AdaptiveWindowProvider(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalAdaptiveWindowMode provides adaptiveWindowMode(maxWidth),
            content = content,
        )
    }
}

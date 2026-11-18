package com.impactomsn.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import android.view.ViewGroup
import com.example.liquidglass.BlurMethod
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassView

/**
 * A real per-surface Liquid Glass container.
 *
 * The glass view owns a ComposeView child, so the lens is rendered first and
 * the actual text/images stay sharp above it. The backdrop is the nearest
 * Android parent, never the DecorView/root, avoiding RenderNode recursion.
 */
@Composable
internal fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadiusDp: Float = 28f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val currentContent by rememberUpdatedState(content)
    AndroidView(
        modifier = modifier,
        factory = {
            LiquidGlassView(context).apply {
                enableBackdropBlur = true
                enableChromaticAberration = true
                enableChromaticDispersion = false
                enableEdgeHighlight = true
                blurMethod = BlurMethod.BOX_BLUR
                globalDownsampleFactor = 0.65f
                material = GlassMaterial.REGULAR
                displacementScale = 22f
                blurAmount = 0.06f
                saturation = 112f
                aberrationIntensity = 0.8f
                cornerRadius = cornerRadiusDp * resources.displayMetrics.density
                elevation = 4f * resources.displayMetrics.density
                isClickable = onClick != null
                setOnClickListener { onClick?.invoke() }
                addView(ComposeView(context).also { child ->
                    child.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    child.setContent {
                    currentContent()
                    }
                })
            }
        },
        update = { glass ->
            // The immediate parent is the safe backdrop source. LiquidGlassView
            // excludes itself while capturing, so sibling content is not lost.
            glass.backdropSource = glass.parent as? android.view.View
            glass.invalidate()
        },
    )
}

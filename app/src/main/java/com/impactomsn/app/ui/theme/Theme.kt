package com.impactomsn.app.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.impactomsn.app.AppAppearance
import com.impactomsn.app.AppearancePrefs
import com.impactomsn.app.ColorMode
import com.impactomsn.app.LocalMessengerPlusEnabled
import com.impactomsn.app.LocalMessengerPlusHideControlCharacters
import com.impactomsn.app.LocalMessengerScene
import com.impactomsn.app.LocalScenePresentation
import com.impactomsn.app.MessengerPlusPrefs
import com.impactomsn.app.ScenePresentation
import com.impactomsn.app.UiStyle
import com.impactomsn.app.messengerScene
import com.impactomsn.app.pages.components.KeyboardSheetScaffold

internal val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = DarkInversePrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkPrimary,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Scrim,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)

internal val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = LightInversePrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightPrimary,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Scrim,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ImpactoMsnTheme(
    fillWindowSurface: Boolean = true,
    updateSystemBars: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val appearanceFlow = remember(context) { AppearancePrefs.state(context) }
    val appearance by appearanceFlow.collectAsState()
    val messengerPlusFlow = remember(context) { MessengerPlusPrefs.state(context) }
    val messengerPlusSettings by messengerPlusFlow.collectAsState()
    val isDark = false // dark mode was removed
    val aero = appearance.uiStyle == UiStyle.Aero
    val liquidGlass = appearance.uiStyle == UiStyle.LiquidGlass
    val colorScheme = remember(context, appearance, isDark) {
        impactomsnColorScheme(context, appearance, isDark).let {
            when {
                aero -> aeroColorScheme(it)
                liquidGlass -> liquidGlassColorScheme(it)
                else -> it
            }
        }
    }
    val view = LocalView.current

    if (updateSystemBars && !view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalMessengerScene provides if (appearance.colorMode == ColorMode.Scenes) messengerScene(appearance.sceneId) else null,
        LocalScenePresentation provides ScenePresentation(
            showInConversations = appearance.showSceneInConversations,
            darkTint = isDark && appearance.darkenScenesInDarkMode,
            isDark = isDark,
        ),
        LocalMessengerPlusEnabled provides messengerPlusSettings.enabled,
        LocalMessengerPlusHideControlCharacters provides messengerPlusSettings.shouldHideControlCharacters,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = ImpactoMsnShapes,
        ) {
            AdaptiveWindowProvider {
                if (fillWindowSurface && aero) {
                    Box(Modifier.fillMaxSize().background(AeroBackdrop)) {
                        KeyboardSheetScaffold(content = content)
                    }
                } else if (fillWindowSurface && liquidGlass) {
                    Box(Modifier.fillMaxSize().background(liquidGlassBackdrop(colorScheme))) {
                        KeyboardSheetScaffold(content = content)
                    }
                } else if (fillWindowSurface) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colorScheme.surfaceContainer,
                    ) {
                        KeyboardSheetScaffold(content = content)
                    }
                } else {
                    content()
                }
            }
        }
    }
}

internal fun impactomsnColorScheme(
    context: Context,
    appearance: AppAppearance,
    isDark: Boolean,
): ColorScheme {
    val useDynamic = appearance.colorMode == ColorMode.Dynamic &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        appearance.colorMode == ColorMode.Scenes -> seededColorScheme(
            appearance.sceneColorArgb ?: messengerScene(appearance.sceneId).seedArgb, isDark)
        appearance.colorMode == ColorMode.Default && appearance.customColorArgb != null ->
            seededColorScheme(appearance.customColorArgb, isDark)
        useDynamic && isDark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }
}

/** Windows 7 Aero sky: the glass surfaces below are translucent and sit on top of this gradient. */
internal val AeroBackdrop = Brush.verticalGradient(
    0f to Color(0xFFB4CFEA),
    0.45f to Color(0xFFE6F0FA),
    1f to Color(0xFFBFD6EE),
)

/** Aero Glass palette: sky-blue accents, dark navy text and translucent white containers. */
internal fun aeroColorScheme(base: ColorScheme): ColorScheme = base.copy(
    // Keep the selected Default/Dynamic/Scenes seed instead of replacing it
    // with a fixed blue palette.
    primary = base.primary,
    onPrimary = base.onPrimary,
    primaryContainer = base.primaryContainer.copy(alpha = 0.80f),
    onPrimaryContainer = base.onPrimaryContainer,
    secondary = base.secondary,
    onSecondary = base.onSecondary,
    secondaryContainer = base.secondaryContainer.copy(alpha = 0.70f),
    onSecondaryContainer = base.onSecondaryContainer,
    background = Color(0xFFD6E5F5),
    onBackground = Color(0xFF1B2D44),
    surface = Color(0xB3F4F8FD),
    onSurface = Color(0xFF1B2D44),
    surfaceVariant = Color(0x99E3EEF9),
    onSurfaceVariant = Color(0xFF3D546E),
    outline = Color(0xFF8AA6C4),
    outlineVariant = Color(0x99FFFFFF),
    surfaceContainerLowest = Color(0x99FFFFFF),
    surfaceContainerLow = Color(0xA6F1F6FC),
    surfaceContainer = Color(0xB3E8F0FA),
    surfaceContainerHigh = Color(0xCCDDE9F6),
    surfaceContainerHighest = Color(0xDDD2E2F3),
)

/**
 * Apple-inspired translucent material implemented in Compose so it also works
 * on iOS/iPadOS 15 and older macOS versions without Apple's newer native API.
 */
internal fun liquidGlassBackdrop(base: ColorScheme): Brush = Brush.verticalGradient(
    0f to base.primary.copy(alpha = 0.18f),
    0.42f to Color.White.copy(alpha = 0.92f),
    1f to base.secondary.copy(alpha = 0.16f),
)

internal fun liquidGlassColorScheme(base: ColorScheme): ColorScheme = base.copy(
    // The glass treatment changes translucency and surfaces, while the
    // selected color mode supplies the accent/hue.
    primary = base.primary,
    onPrimary = base.onPrimary,
    primaryContainer = base.primaryContainer.copy(alpha = 0.72f),
    onPrimaryContainer = base.onPrimaryContainer,
    secondary = base.secondary,
    onSecondary = base.onSecondary,
    secondaryContainer = base.secondaryContainer.copy(alpha = 0.60f),
    onSecondaryContainer = base.onSecondaryContainer,
    background = Color(0xFFEAF1FC),
    onBackground = Color(0xFF18243A),
    surface = Color(0x99FFFFFF),
    onSurface = Color(0xFF18243A),
    surfaceVariant = Color(0x80FFFFFF),
    onSurfaceVariant = Color(0xFF4A5B76),
    outline = Color(0x806D86AD),
    outlineVariant = Color(0x66FFFFFF),
    surfaceContainerLowest = Color(0x66FFFFFF),
    surfaceContainerLow = Color(0x80FFFFFF),
    surfaceContainer = Color(0x99FFFFFF),
    surfaceContainerHigh = Color(0xB3FFFFFF),
    surfaceContainerHighest = Color(0xCCFFFFFF),
)

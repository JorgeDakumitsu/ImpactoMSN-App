package com.impactomsn.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colours sampled from Windows Live Messenger 8.5 screenshots (Aero-blue chrome, neutral grey content). */
object MsnPalette {
    val TitleBlue = Color(0xFF438DC1)      // window title bar
    val TitleBlueDeep = Color(0xFF3F83B5)  // toolbar row under the title
    val SkyTop = Color(0xFF55ADEF)         // top of the contact-list / chat header "sky"
    val SkyFade = Color(0xFFE5EAEF)        // where the sky fades into the content
    val Toolbar = Color(0xFFD7E5EF)
    val Content = Color(0xFFEFEFEF)        // contact list / login background
    val ChatBody = Color(0xFFE0E4E8)
    val Input = Color(0xFFE8E8E8)
    val Selected = Color(0xFFD6D6D6)       // selected contact row
    val GlowTop = Color(0xFFC1DBEF)        // blue glow at the bottom of the window
    val GlowBottom = Color(0xFFAAD2EF)
    val Link = Color(0xFF0066B3)
    val Ink = Color(0xFF0B3A52)
    val Online = Color(0xFF3CA83C)
    val Offline = Color(0xFF8A8A8A)
    val Busy = Color(0xFFD33B2C)
    val Away = Color(0xFFE5A21B)
}

/** True when the active Material scheme is a dark one: the glossy light chrome is skipped there. */
@Composable
fun isMsnDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** Sky at the top, neutral grey content, blue glow at the bottom - the shape of every WLM 8.5 window. */
@Composable
fun Modifier.msnWindowBackground(): Modifier =
    if (isMsnDark()) this else this.background(
        Brush.verticalGradient(
            0.00f to MsnPalette.SkyTop,
            0.10f to MsnPalette.SkyFade,
            0.22f to MsnPalette.Content,
            0.78f to MsnPalette.Content,
            1.00f to MsnPalette.GlowBottom,
        ),
    )

/** Glossy rounded blue frame the classic client draws around display pictures. */
@Composable
fun MsnAvatarFrame(modifier: Modifier = Modifier, size: Dp = 120.dp, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(size * 0.14f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFBFE0F8), Color(0xFF4F9AD3))))
            .border(2.dp, Color(0xFF95C4E6), shape)
            .padding(size * 0.07f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size * 0.86f).clip(RoundedCornerShape(size * 0.08f)).background(Color(0xFFF4F8FC)),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

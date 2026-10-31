package za.co.rc.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import za.co.rc.tv.core.focusScale

val LocalFocusAnimation=staticCompositionLocalOf { "Off" }

/** One focus transform per surface. No layout reflow, timer, bitmap blur or parent animation state. */
internal fun Modifier.cinematicFocus(radius: Dp = 14.dp): Modifier = composed {
    val intensity=LocalFocusAnimation.current; val palette=LocalAppearance.current
    var focused by remember { mutableStateOf(false) }
    val enabled=intensity!="Off"
    // Animate one shared lift value so scale, elevation and edge arrive together.
    // A short, bounded spring makes the change of selection visible on a distant TV.
    val lift by animateFloatAsState(if(focused && enabled) 1f else 0f,
        if(enabled) spring(dampingRatio=if(intensity=="Enhanced") .65f else .72f, stiffness=650f) else snap(),label="cinematicFocus")
    val strength=when(intensity) { "Enhanced" -> 2f; "Subtle" -> .65f; else -> 1f }
    this.onFocusChanged { focused=it.isFocused }
        .zIndex(if(focused) 1f else 0f)
        .graphicsLayer {
            val scale=1f+(focusScale(intensity)-1f)*lift
            scaleX=scale; scaleY=scale; clip=false; shape=RoundedCornerShape(radius)
            translationY=-(if(intensity=="Enhanced") 6.dp else 2.dp*strength).toPx()*lift
            shadowElevation=10.dp.toPx()*strength*lift.coerceAtLeast(0f)
            ambientShadowColor=palette.halo.copy(alpha=.5f); spotShadowColor=palette.halo.copy(alpha=.5f)
        }.drawWithCache {
            val glow=Brush.radialGradient(listOf(palette.halo.copy(alpha=.28f*strength),Color.Transparent))
            onDrawBehind {
                if(lift>0f && enabled) {
                    val edge=2.dp.toPx()
                    drawRoundRect(glow,Offset(-6.dp.toPx(),-6.dp.toPx()),Size(size.width+12.dp.toPx(),size.height+12.dp.toPx()),CornerRadius(radius.toPx()),alpha=lift.coerceIn(0f,1f))
                    drawRoundRect(palette.focus.copy(alpha=.75f*lift.coerceIn(0f,1f)),Offset(-edge,-edge),Size(size.width+edge*2,size.height+edge*2),CornerRadius(radius.toPx()+edge),style=Stroke(1.5.dp.toPx()))
                }
            }
        }
}

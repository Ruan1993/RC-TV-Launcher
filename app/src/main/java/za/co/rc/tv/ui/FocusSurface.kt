package za.co.rc.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
    val scale by animateFloatAsState(if(focused) focusScale(intensity) else 1f,
        if(enabled) tween(140) else snap(),label="cinematicFocus")
    this.onFocusChanged { focused=it.isFocused }
        .graphicsLayer {
            scaleX=scale; scaleY=scale; clip=false; shape=RoundedCornerShape(radius)
            shadowElevation=if(focused && enabled) (if(intensity=="Enhanced") 5.dp else 3.dp).toPx() else 0f
            ambientShadowColor=palette.halo.copy(alpha=.24f); spotShadowColor=palette.halo.copy(alpha=.24f)
        }.drawWithCache {
            val glow=Brush.radialGradient(listOf(palette.halo.copy(alpha=if(intensity=="Enhanced") .20f else .12f),Color.Transparent))
            onDrawBehind { if(focused && enabled) drawRoundRect(glow,Offset(-5.dp.toPx(),-5.dp.toPx()),Size(size.width+10.dp.toPx(),size.height+10.dp.toPx()),CornerRadius(radius.toPx())) }
        }
}

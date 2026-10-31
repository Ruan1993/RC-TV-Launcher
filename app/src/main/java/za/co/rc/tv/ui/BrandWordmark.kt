package za.co.rc.tv.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp

/** Original geometric lettering, cached native paths; no font or bitmap dependency. */
@Composable internal fun BrandWordmark() {
    val palette=LocalAppearance.current
    val density=LocalDensity.current
    Box(Modifier.size(with(density) { 84.sp.toDp() },with(density) { 25.sp.toDp() })
        .semantics { contentDescription="RC TV" }
        .drawWithCache {
            val lettering=Path().apply {
                fillType=PathFillType.EvenOdd
                // R: chamfered bowl and diagonal leg, with a transparent counter.
                moveTo(0f,0f); lineTo(24f,0f); lineTo(30f,6f); lineTo(30f,17f); lineTo(24f,23f)
                lineTo(34f,40f); lineTo(23f,40f); lineTo(13f,24f); lineTo(9f,24f); lineTo(9f,40f); lineTo(0f,40f); close()
                moveTo(9f,8f); lineTo(21f,8f); lineTo(22f,9f); lineTo(22f,15f); lineTo(21f,16f); lineTo(9f,16f); close()
                // C, T and V use the same stroke weight and cut corners.
                moveTo(44f,0f); lineTo(65f,0f); lineTo(72f,7f); lineTo(66f,13f); lineTo(61f,8f)
                lineTo(49f,8f); lineTo(45f,12f); lineTo(45f,28f); lineTo(49f,32f); lineTo(61f,32f)
                lineTo(66f,27f); lineTo(72f,33f); lineTo(65f,40f); lineTo(44f,40f); lineTo(36f,32f); lineTo(36f,8f); close()
                moveTo(90f,0f); lineTo(122f,0f); lineTo(122f,8f); lineTo(110f,8f); lineTo(110f,40f)
                lineTo(102f,40f); lineTo(102f,8f); lineTo(90f,8f); close()
                moveTo(127f,0f); lineTo(136f,0f); lineTo(146f,28f); lineTo(156f,0f); lineTo(165f,0f)
                lineTo(150f,40f); lineTo(142f,40f); close()
            }
            val metal=Brush.verticalGradient(listOf(Color.White,palette.accent,Color.White,palette.halo),startY=0f,endY=40f)
            onDrawBehind {
                val unit=size.height/50f
                withTransform({ translate(1f*unit,5f*unit); scale(unit,unit,pivot=androidx.compose.ui.geometry.Offset.Zero) }) {
                    drawPath(lettering,palette.halo.copy(alpha=.12f),style=Stroke(4f))
                    drawPath(lettering,Color.Black.copy(alpha=.85f),style=Stroke(1.6f))
                    drawPath(lettering,metal)
                }
            }
        })
}

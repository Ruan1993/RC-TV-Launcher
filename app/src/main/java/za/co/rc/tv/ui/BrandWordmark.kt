package za.co.rc.tv.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/** Native lettering only: same maximum type size as the old heading, no asset/font load. */
@Composable internal fun BrandWordmark() {
    val palette=LocalAppearance.current
    val metal=Brush.linearGradient(listOf(Color.White,palette.accent,Color.White,palette.halo))
    Text(buildAnnotatedString {
        withStyle(SpanStyle(fontWeight=FontWeight.Black,fontStyle=FontStyle.Italic,letterSpacing=(-.8).sp)) { append("RC") }
        withStyle(SpanStyle(fontSize=13.sp,fontWeight=FontWeight.Medium,letterSpacing=2.sp)) { append("  TV") }
    },style=floatingTextStyle().copy(brush=metal,fontFamily=FontFamily.SansSerif,
        shadow=Shadow(Color.Black.copy(alpha=.95f),Offset(0f,2f),5f)),
        fontSize=20.sp,maxLines=1,softWrap=false)
}

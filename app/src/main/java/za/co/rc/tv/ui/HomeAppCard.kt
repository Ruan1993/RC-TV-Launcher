package za.co.rc.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import za.co.rc.tv.R
import za.co.rc.tv.core.AppRepository
import za.co.rc.tv.core.LauncherApp
import za.co.rc.tv.core.RenderDiagnostics

/** Home-only material. Cached brushes; no blur, shadow bitmap or per-frame layout. */
@Composable
internal fun HomeAppCard(app: LauncherApp, repository: AppRepository, generation: Int,
    motion: String, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    SideEffect { RenderDiagnostics.tileComposed() }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused && motion != "Minimal") if (motion == "Cinematic") 1.055f else 1.035f else 1f,
        if (motion == "Minimal") snap() else spring(dampingRatio = if (motion == "Cinematic") .78f else .9f, stiffness = 500f), label = "homeCardFocus")
    val icon by produceState<android.graphics.Bitmap?>(null, app.packageName, generation) {
        value = null
        value = repository.icon(app.packageName, sizeDp = 80)
    }
    val shape = RoundedCornerShape(26.dp)
    Box(Modifier.width(232.dp).padding(horizontal = 8.dp, vertical = 10.dp)) {
        Button(onLaunch, Modifier.then(modifier).fillMaxWidth().height(190.dp)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawWithCache {
                val corners = CornerRadius(26.dp.toPx())
                val material = Brush.verticalGradient(listOf(
                    Color(0xFFDFEFF2).copy(alpha = .18f),
                    Color(0xFFB6CED2).copy(alpha = .075f),
                    Color(0xFF15232D).copy(alpha = .30f)))
                val edge = Brush.linearGradient(listOf(Color.White.copy(alpha = .50f),
                    Color.White.copy(alpha = .08f), Color(0xFFBDDCD9).copy(alpha = .27f)))
                val reflection = Brush.verticalGradient(listOf(Color.White.copy(alpha = .10f), Color.Transparent))
                val halo = Brush.radialGradient(listOf(Color(0xFFBDE8DE).copy(alpha = .20f), Color.Transparent))
                onDrawBehind {
                    if (focused && motion != "Minimal") drawRoundRect(halo, Offset(-7.dp.toPx(), -7.dp.toPx()),
                        Size(size.width + 14.dp.toPx(), size.height + 14.dp.toPx()), corners)
                    drawRoundRect(material, cornerRadius = corners)
                    drawRoundRect(reflection, Offset(7.dp.toPx(), 7.dp.toPx()),
                        Size(size.width - 14.dp.toPx(), size.height * .42f), CornerRadius(20.dp.toPx()))
                    drawRoundRect(edge, cornerRadius = corners, style = Stroke(1.dp.toPx()))
                    if (focused) drawRoundRect(Color(0xFFD5F5EB), cornerRadius = corners, style = Stroke(2.dp.toPx()))
                }
            }, shape = shape, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(20.dp)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                if (icon != null) Image(icon!!.asImageBitmap(), null, Modifier.size(80.dp))
                else Image(painterResource(R.drawable.rc_icon), null, Modifier.size(80.dp))
                Spacer(Modifier.height(22.dp))
                Text(app.label, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

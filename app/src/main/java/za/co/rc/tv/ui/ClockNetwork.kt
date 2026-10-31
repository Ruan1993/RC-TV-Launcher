package za.co.rc.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import za.co.rc.tv.core.*

@Composable internal fun floatingTextStyle(): TextStyle = androidx.compose.material3.LocalTextStyle.current.copy(shadow=Shadow(Color.Black.copy(alpha=.9f),Offset(0f,2f),5f))

@Composable internal fun ClockNetwork(clock: String, monitor: NetworkStatusMonitor, focus: FocusRequester, settings: FocusRequester, open: () -> Unit) {
    val status by monitor.state.collectAsState(); val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,monitor) {
        val observer=LifecycleEventObserver { _,event -> when(event) { Lifecycle.Event.ON_RESUME -> monitor.start(); Lifecycle.Event.ON_PAUSE -> monitor.stop(); else -> Unit } }
        lifecycle.addObserver(observer); if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) monitor.start()
        onDispose { lifecycle.removeObserver(observer); monitor.stop() }
    }
    var focused by remember { mutableStateOf(false) }; val palette=LocalAppearance.current
    Row(Modifier.focusRequester(focus).focusProperties { up=settings; right=settings }.onFocusChanged { focused=it.isFocused }
        .cinematicFocus(12.dp).border(1.dp,if(focused) palette.focus else Color.Transparent,RoundedCornerShape(12.dp))
        .semantics(mergeDescendants=true) { contentDescription="$clock. ${status.description}. Open Connections & Devices" }
        .clickable(onClick=open).padding(horizontal=6.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        NetworkMark(status,Modifier.size(24.dp))
        Text(clock,fontSize=28.sp,lineHeight=34.sp,style=floatingTextStyle(),color=Color.White)
    }
}

@Composable private fun NetworkMark(status: NetworkStatus,modifier: Modifier) {
    val colour=LocalAppearance.current.accent
    Canvas(modifier) {
        val w=size.width; val h=size.height; val stroke=1.7.dp.toPx()
        when(status.kind) {
            NetworkKind.ETHERNET -> { drawRoundRect(colour,Offset(w*.15f,h*.14f),Size(w*.7f,h*.5f),androidx.compose.ui.geometry.CornerRadius(w*.08f),style=Stroke(stroke)); for(x in listOf(.32f,.5f,.68f)) drawLine(colour,Offset(w*x,h*.23f),Offset(w*x,h*.38f),stroke); drawLine(colour,Offset(w*.5f,h*.64f),Offset(w*.5f,h*.87f),stroke); drawLine(colour,Offset(w*.32f,h*.87f),Offset(w*.68f,h*.87f),stroke) }
            NetworkKind.WIFI,NetworkKind.WIFI_OFF,NetworkKind.WIFI_DISCONNECTED -> {
                val active=status.kind==NetworkKind.WIFI
                val tint=if(status.kind==NetworkKind.WIFI_OFF) colour.copy(alpha=.45f) else colour
                drawCircle(tint,w*.055f,Offset(w*.5f,h*.82f))
                for(i in 1..3) { val radius=w*(.14f+i*.12f); val lit=active && status.level!=null && status.level>=i
                    drawArc(tint.copy(alpha=if(lit) 1f else .28f),225f,90f,false,Offset(w*.5f-radius,h*.82f-radius),Size(radius*2,radius*2),style=Stroke(stroke)) }
                if(!active) drawLine(tint,Offset(w*.16f,h*.18f),Offset(w*.86f,h*.88f),stroke)
                if(active && status.level==null) drawCircle(colour,w*.035f,Offset(w*.88f,h*.22f),style=Stroke(stroke))
            }
            else -> { drawCircle(colour.copy(alpha=.65f),w*.32f,style=Stroke(stroke)); drawLine(colour,Offset(w*.39f,h*.5f),Offset(w*.61f,h*.5f),stroke) }
        }
    }
}

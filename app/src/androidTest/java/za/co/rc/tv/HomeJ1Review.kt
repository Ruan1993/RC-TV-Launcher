package za.co.rc.tv
import android.app.*
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.io.File
import za.co.rc.tv.core.*

object HomeJ1Review {
    fun run(test: Instrumentation,args: Bundle) {
        val prefs=Preferences(test.targetContext); val before=runBlocking { prefs.state.first() }
        try {
            runBlocking { prefs.setTextSize(if(args.getString("large")=="true") "Extra Large" else "Standard"); prefs.setFocusAnimation(args.getString("intensity","Standard")); prefs.setReducedMotion(args.getString("reduced")=="true") }
            val activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1400); val ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)!!
            test.runOnMainSync { activity.window.decorView.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) }
            Thread.sleep(200)
            if(android.os.Build.VERSION.SDK_INT>=33) ui.clearCache()
            val root=ui.rootInActiveWindow ?: error("No Home")
            var cards=0; var settings: Rect?=null; val labels=mutableListOf<String>()
            fun visit(node: AccessibilityNodeInfo) {
                val rect=Rect(); node.getBoundsInScreen(rect); node.text?.toString()?.let { labels+=it }
                if(node.className?.toString()=="android.widget.Button" && rect.width()==432 && rect.height()==380) cards++
                if(node.contentDescription?.toString()=="Settings") settings=rect
                for(i in 0 until node.childCount) node.getChild(i)?.let { visit(it); it.recycle() }
            }
            visit(root); root.recycle(); check(cards>0); check(labels.none { it.contains("More controls") || it.contains("D-pad to explore") })
            val rect=settings ?: error("No fixed Settings"); check(rect.right<=1920 && rect.left>1300 && rect.top<180)
            val bmp=ui.takeScreenshot() ?: error("No screenshot")
            File(test.targetContext.getExternalFilesDir(null),"j1-home.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG,100,it) }; bmp.recycle()
            fun find(item: AccessibilityNodeInfo,predicate: (AccessibilityNodeInfo)->Boolean): AccessibilityNodeInfo? {
                if(predicate(item)) return AccessibilityNodeInfo.obtain(item)
                for(i in 0 until item.childCount) item.getChild(i)?.let { child -> val value=find(child,predicate); child.recycle(); if(value!=null) return value }; return null
            }
            fun query(predicate: (AccessibilityNodeInfo)->Boolean): AccessibilityNodeInfo {
                if(android.os.Build.VERSION.SDK_INT>=33) ui.clearCache()
                val tree=ui.rootInActiveWindow ?: error("No window"); val result=find(tree,predicate); tree.recycle(); return result ?: error("Missing navigation control")
            }
            fun focusedBelow(item: AccessibilityNodeInfo): Boolean {
                find(item) { it.isFocused }?.let { it.recycle(); return true }
                var parent=item.parent
                repeat(2) { val current=parent ?: return false; val yes=current.isFocused; parent=current.parent; current.recycle(); if(yes) { parent?.recycle(); return true } }
                parent?.recycle(); return false
            }
            val settingsNode=query { it.contentDescription?.toString()=="Settings" }
            var clickable=find(settingsNode) { it.isClickable }
            var parent=settingsNode.parent
            repeat(4) { if(clickable==null && parent!=null) { val current=parent!!; if(current.isClickable) clickable=AccessibilityNodeInfo.obtain(current); parent=current.parent; current.recycle() } }
            parent?.recycle(); settingsNode.recycle()
            val control=clickable ?: error("Settings not focusable")
            fun key(code: Int) { val now=android.os.SystemClock.uptimeMillis(); ui.injectInputEvent(KeyEvent(now,now,KeyEvent.ACTION_DOWN,code,0),true); ui.injectInputEvent(KeyEvent(now,now+20,KeyEvent.ACTION_UP,code,0),true); Thread.sleep(350) }
            key(KeyEvent.KEYCODE_DPAD_UP)
            check(control.performAction(AccessibilityNodeInfo.ACTION_FOCUS)); control.recycle(); Thread.sleep(100)
            key(KeyEvent.KEYCODE_DPAD_LEFT)
            query { it.contentDescription?.toString()=="Speed Test" }.let { check(focusedBelow(it)); it.recycle() }
            key(KeyEvent.KEYCODE_DPAD_RIGHT)
            query { it.contentDescription?.toString()=="Settings" }.let { check(focusedBelow(it)); it.recycle() }
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            query { it.contentDescription?.toString()?.contains("Open Connections & Devices")==true }.let { check(focusedBelow(it)); it.recycle() }
            key(KeyEvent.KEYCODE_DPAD_UP)
            query { it.contentDescription?.toString()=="Settings" }.let { check(focusedBelow(it)); it.recycle() }
            val bytes=test.targetContext.filesDir.resolve("datastore/launcher.preferences_pb").readBytes().toString(Charsets.ISO_8859_1)
            check(bytes.contains("focus_animation") && bytes.contains(args.getString("intensity","Standard"))) { "Focus choice not persisted" }
            test.runOnMainSync { activity.finish() }
        } catch(failure: Exception) {
            val ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)!!
            ui.takeScreenshot()?.let { bmp -> File(test.targetContext.getExternalFilesDir(null),"j1-failure.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG,100,it) }; bmp.recycle() }
            throw failure
        } finally { runBlocking { prefs.setTextSize(before.textSize); prefs.setFocusAnimation(before.focusAnimation); prefs.setReducedMotion(before.reducedMotion) } }
    }
}

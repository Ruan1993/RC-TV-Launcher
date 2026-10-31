package za.co.rc.tv

import android.app.Instrumentation
import android.content.Intent
import android.app.UiAutomation
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import za.co.rc.tv.core.*

/** Emulator-only actual Settings interaction, including focus restoration and utility Back. */
object SettingsReviewCheck {
    fun run(test: Instrumentation, large: Boolean) {
        val prefs=Preferences(test.targetContext); val before=runBlocking { prefs.state.first() }
        var capture: UiAutomation?=null
        try {
            if(large) runBlocking { prefs.setTextSize("Extra Large"); prefs.setFontStyle("Bold / High Readability") }
            val activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)!!.also { capture=it }
            test.runOnMainSync { activity.window.decorView.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) }
            fun key(code: Int) { val time=SystemClock.uptimeMillis(); ui.injectInputEvent(KeyEvent(time,time,KeyEvent.ACTION_DOWN,code,0),true); ui.injectInputEvent(KeyEvent(time,time+15,KeyEvent.ACTION_UP,code,0),true); Thread.sleep(180) }
            fun node(text: String): AccessibilityNodeInfo? {
                if(android.os.Build.VERSION.SDK_INT>=33) ui.clearCache()
                val root=ui.rootInActiveWindow ?: return null
                fun walk(item: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                    if(item.text?.toString()?.trim()==text || item.contentDescription?.toString()?.trim()==text) return AccessibilityNodeInfo.obtain(item)
                    for(index in 0 until item.childCount) { val child=item.getChild(index) ?: continue; val found=walk(child); child.recycle(); if(found!=null) return found }
                    return null
                }
                val result=walk(root); root.recycle(); return result
            }
            fun await(text: String): AccessibilityNodeInfo {
                repeat(40) { node(text)?.let { return it }; Thread.sleep(75) }; error("Settings element missing: $text")
            }
            fun actionable(text: String): AccessibilityNodeInfo {
                var found=node(text)
                for(attempt in 0 until 45) { if(found!=null) break; key(KeyEvent.KEYCODE_DPAD_DOWN); found=node(text) }
                val item=found ?: error("D-pad cannot reach: $text")
                fun childButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                    if(node.isClickable) return AccessibilityNodeInfo.obtain(node)
                    for(index in 0 until node.childCount) { val child=node.getChild(index) ?: continue; val match=childButton(child); child.recycle(); if(match!=null) return match }; return null
                }
                childButton(item)?.let { item.recycle(); return it }
                var current=item
                repeat(5) { val parent=current.parent; if(parent!=null) { current.recycle(); current=parent; if(current.isClickable) return current } }
                current.recycle(); error("No actionable control: $text")
            }
            fun click(text: String) {
                val target=actionable(text)
                target.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id); Thread.sleep(120)
                check(target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Cannot select $text" }; target.recycle(); Thread.sleep(150)
            }
            fun visible(text: String) { val target=actionable(text); target.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id); target.performAction(AccessibilityNodeInfo.ACTION_FOCUS); target.recycle(); Thread.sleep(100) }
            fun focused(text: String) { repeat(30) { val item=actionable(text); val ok=item.isFocused; item.recycle(); if(ok) return; Thread.sleep(75) }; error("Category focus not restored: $text") }
            fun shot(name: String) { val image=ui.takeScreenshot() ?: error("No screenshot"); File(test.targetContext.getExternalFilesDir(null),name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle() }
            await("Home").recycle()
            var speed=actionable("Speed Test"); speed.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id); speed.recycle()
            Thread.sleep(450)
            speed=actionable("Speed Test")
            if(!speed.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                speed.recycle(); key(KeyEvent.KEYCODE_DPAD_UP)
                repeat(10) { key(KeyEvent.KEYCODE_DPAD_RIGHT) }
            } else { speed.recycle(); key(KeyEvent.KEYCODE_DPAD_RIGHT) }
            click("Settings")
            await("Appearance & Personalisation").recycle(); shot("settings-categories.png")
            click("Appearance & Personalisation"); visible("Choose wallpaper"); click("Choose wallpaper")
            await("Choose a scene. Preview before applying.").recycle(); key(KeyEvent.KEYCODE_BACK)
            visible("Choose wallpaper"); visible("Reset to Original"); visible("Reduced motion: off"); key(KeyEvent.KEYCODE_BACK)
            focused("Appearance & Personalisation")
            click("Connections & Devices"); visible("Manage paired phones"); click("Internet Speed Test")
            visible("Start Test"); key(KeyEvent.KEYCODE_BACK); visible("Manage paired phones"); key(KeyEvent.KEYCODE_BACK); focused("Connections & Devices")
            click("Apps & Media"); visible("Android app settings"); click("Browse installed apps"); key(KeyEvent.KEYCODE_BACK)
            visible("Android app settings"); key(KeyEvent.KEYCODE_BACK); focused("Apps & Media")
            click("Weather & Widgets"); visible("About weather & privacy"); shot("settings-weather.png"); key(KeyEvent.KEYCODE_BACK); focused("Weather & Widgets")
            click("System & Updates")
            for(label in listOf("Easy setup","Android settings","Phone control permissions","Try keyboard","Enable in Android…","Choose keyboard…","Choose RC TV as Home…","Open Home settings / restore")) visible(label)
            shot("settings-system.png"); key(KeyEvent.KEYCODE_BACK); focused("System & Updates"); key(KeyEvent.KEYCODE_BACK)
            test.runOnMainSync { activity.finish() }
        } catch(failure: Exception) {
            capture?.takeScreenshot()?.let { bitmap -> File(test.targetContext.getExternalFilesDir(null),"settings-failure.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle() }
            capture?.rootInActiveWindow?.let { root ->
                val labels=StringBuilder()
                fun walk(item: AccessibilityNodeInfo) { labels.append("${item.className} | ${item.text} | ${item.contentDescription}\n"); for(index in 0 until item.childCount) item.getChild(index)?.let { walk(it); it.recycle() } }
                walk(root); root.recycle(); File(test.targetContext.getExternalFilesDir(null),"settings-tree.txt").writeText(labels.toString())
            }
            throw failure
        } finally { runBlocking { prefs.setTextSize(before.textSize); prefs.setFontStyle(before.fontStyle) } }
    }
}

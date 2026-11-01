package za.co.rc.tv

import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.MutableLongState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import za.co.rc.tv.core.*
import java.io.File

/** Physical-device integration. The test APK alone accelerates monotonic idle age. */
object ScreensaverReview {
    fun run(test: Instrumentation, large: Boolean) {
        val prefs=Preferences(test.targetContext)
        val before=runBlocking { prefs.state.first() }
        val ui=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)!!
        var activity: MainActivity?=null
        fun key(code: Int) {
            val now=SystemClock.uptimeMillis()
            ui.injectInputEvent(KeyEvent(now,now,KeyEvent.ACTION_DOWN,code,0),true)
            ui.injectInputEvent(KeyEvent(now,now+20,KeyEvent.ACTION_UP,code,0),true)
            Thread.sleep(250)
        }
        fun find(predicate: (AccessibilityNodeInfo)->Boolean): AccessibilityNodeInfo? {
            if(android.os.Build.VERSION.SDK_INT>=33) ui.clearCache()
            fun walk(item: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if(predicate(item)) return AccessibilityNodeInfo.obtain(item)
                for(i in 0 until item.childCount) item.getChild(i)?.let { child ->
                    val result=walk(child); child.recycle(); if(result!=null) return result
                }
                return null
            }
            val root=ui.rootInActiveWindow ?: return null
            val result=walk(root); root.recycle(); return result
        }
        fun node(label: String) = find { it.text?.toString()==label || it.contentDescription?.toString()==label }
        fun await(label: String): AccessibilityNodeInfo {
            repeat(50) { node(label)?.let { return it }; Thread.sleep(100) }
            error("Missing $label")
        }
        fun control(label: String): AccessibilityNodeInfo {
            var found=node(label)
            for(attempt in 0 until 24) { if(found!=null) break; key(KeyEvent.KEYCODE_DPAD_DOWN); found=node(label) }
            for(attempt in 0 until 24) { if(found!=null) break; key(KeyEvent.KEYCODE_DPAD_UP); found=node(label) }
            var item=found ?: error("D-pad cannot reach $label")
            if(!item.isClickable) {
                for(i in 0 until item.childCount) item.getChild(i)?.let { child ->
                    if(child.isClickable) { item.recycle(); return child }; child.recycle()
                }
                repeat(5) {
                    val parent=item.parent ?: error("No control $label")
                    item.recycle(); item=parent
                    if(item.isClickable) return item
                }
            }
            return item
        }
        fun click(label: String) {
            val item=control(label)
            item.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
            Thread.sleep(100)
            item.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            check(item.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Cannot click $label" }
            item.recycle(); Thread.sleep(300)
        }
        fun shot(name: String) {
            val bitmap=ui.takeScreenshot() ?: error("No screenshot")
            File(test.targetContext.filesDir,"j3b-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
        fun expiredIdle() {
            test.runOnMainSync {
                val field=MainActivity::class.java.getDeclaredField("lastInteraction\$delegate").apply { isAccessible=true }
                (field.get(activity) as MutableLongState).longValue=SystemClock.elapsedRealtime()-599_000L
            }
        }
        fun saverPresent() = node("RC TV screensaver")?.let { it.recycle(); true } ?: false
        try {
            runBlocking { prefs.setTextSize(if(large) "Extra Large" else "Standard"); prefs.setScreensaverMinutes(10); if(large) prefs.setReducedMotion(true) }
            activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            Thread.sleep(1500)
            click("Settings"); click("Screensaver & Power")
            await("Start after: 10 minutes").recycle(); shot(if(large) "settings-xl" else "settings")
            click("Start after: 10 minutes"); click("20 minutes")
            runBlocking { kotlinx.coroutines.withTimeout(5000) { prefs.state.first { it.screensaverMinutes==20 } } }
            click("Start after: 20 minutes"); click("10 minutes")
            runBlocking { kotlinx.coroutines.withTimeout(5000) { prefs.state.first { it.screensaverMinutes==10 } } }
            Thread.sleep(500)
            for(style in screensaverStyles) {
                val current=runBlocking { prefs.state.first() }.screensaverStyle
                click("Style: $current"); click(style)
                runBlocking { kotlinx.coroutines.withTimeout(5000) { prefs.state.first { it.screensaverStyle==style } } }
                click("Preview Screensaver"); await("RC TV screensaver").recycle()
                shot((if(large) "xl-" else "")+when(style) { "Clock & Wallpaper" -> "clock"; "Black Screen" -> "black"; else -> "cinematic" })
                key(if(style=="Black Screen") KeyEvent.KEYCODE_BACK else KeyEvent.KEYCODE_DPAD_CENTER)
                check(!saverPresent()) { "Dismissal failed" }
                val focused=control("Preview Screensaver")
                check(focused.isFocused) { "Preview focus not restored" }; focused.recycle()
            }
            for(label in listOf("Open Android power settings","Android ambient mode settings")) {
                click(label); Thread.sleep(700)
                val root=ui.rootInActiveWindow ?: error("No system window")
                check(root.packageName.toString().contains("settings")); root.recycle()
                // System Settings can retain its nested page; explicitly return to the existing task.
                ui.executeShellCommand("am start -n za.co.rc.tv/.MainActivity").close(); Thread.sleep(700)
                await("Use Android setting").recycle()
            }
            key(KeyEvent.KEYCODE_BACK); key(KeyEvent.KEYCODE_BACK)
            await("Home").recycle()
            expiredIdle(); Thread.sleep(300); key(KeyEvent.KEYCODE_DPAD_LEFT)
            Thread.sleep(1200); check(!saverPresent()) { "Input did not reset inactivity" }
            expiredIdle(); Thread.sleep(1800)
            check(saverPresent()) { "Idle deadline did not activate" }
            key(KeyEvent.KEYCODE_DPAD_DOWN); check(!saverPresent())
            // Silent, looped media in the test APK exercises AudioManager without
            // playing audible content or depending on a third-party subscription.
            val track=android.media.AudioTrack.Builder()
                .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).build())
                .setAudioFormat(android.media.AudioFormat.Builder().setSampleRate(48_000)
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(96_000).setTransferMode(android.media.AudioTrack.MODE_STATIC).build()
            try {
                check(track.write(ShortArray(48_000),0,48_000)==48_000)
                check(track.setLoopPoints(0,48_000,-1)==android.media.AudioTrack.SUCCESS)
                track.play(); Thread.sleep(400)
                val audio=test.targetContext.getSystemService(android.media.AudioManager::class.java)
                check(audio.isMusicActive) { "Media fixture not active" }
                expiredIdle(); Thread.sleep(1800)
                check(!saverPresent() && audio.isMusicActive) { "Screensaver interrupted media" }
            } finally { track.stop(); track.release() }
            // Never must suppress the same elapsed deadline.
            runBlocking { prefs.setScreensaverMinutes(0) }; Thread.sleep(200)
            expiredIdle(); Thread.sleep(1500); check(!saverPresent())
            runBlocking { prefs.setScreensaverMinutes(10) }; Thread.sleep(200)
            // A genuine external foreground activity cancels the launcher deadline.
            ui.executeShellCommand("am start -a android.settings.SETTINGS").close(); Thread.sleep(800)
            expiredIdle(); Thread.sleep(1500); check(!saverPresent())
            val external=ui.rootInActiveWindow ?: error("No external window")
            check(external.packageName.toString().contains("settings")); external.recycle()
            ui.executeShellCommand("am start -n za.co.rc.tv/.MainActivity").close(); Thread.sleep(800)
            check(!saverPresent()) { "Return should start a fresh idle period" }
        } catch(failure: Throwable) { shot("failure"); throw failure }
        finally {
            test.runOnMainSync { activity?.finish() }
            runBlocking {
                prefs.setTextSize(before.textSize); prefs.setScreensaverMinutes(before.screensaverMinutes); prefs.setScreensaverStyle(before.screensaverStyle); prefs.setReducedMotion(before.reducedMotion)
                val after=prefs.state.first()
                check(after.favourites==before.favourites && after.favouriteOrder==before.favouriteOrder)
                File(test.targetContext.filesDir,"j3b-preservation.txt").writeText("Favourites: ${after.favourites.size}\nSaved order: unchanged\nAppearance restored: ${after.textSize}\nReduced Motion restored: ${after.reducedMotion}\nScreensaver restored: ${after.screensaverMinutes} minutes / ${after.screensaverStyle}\n")
            }
        }
    }
}

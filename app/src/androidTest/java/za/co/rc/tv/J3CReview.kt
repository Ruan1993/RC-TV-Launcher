package za.co.rc.tv

import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import za.co.rc.tv.core.*
import java.io.File

internal class TvReview(val test: Instrumentation) {
    val ui = test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)!!
    fun key(code: Int) {
        val now=SystemClock.uptimeMillis()
        ui.injectInputEvent(KeyEvent(now,now,KeyEvent.ACTION_DOWN,code,0),true)
        ui.injectInputEvent(KeyEvent(now,now+20,KeyEvent.ACTION_UP,code,0),true)
        Thread.sleep(300)
    }
    fun node(label: String): AccessibilityNodeInfo? {
        if(android.os.Build.VERSION.SDK_INT>=33) ui.clearCache()
        fun walk(item: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if(item.text?.toString()==label || item.contentDescription?.toString()==label) return AccessibilityNodeInfo.obtain(item)
            for(i in 0 until item.childCount) item.getChild(i)?.let { child ->
                val result=walk(child); child.recycle(); if(result!=null) return result
            }
            return null
        }
        val root=ui.rootInActiveWindow ?: return null
        val result=walk(root); root.recycle(); return result
    }
    fun control(label: String): AccessibilityNodeInfo {
        var found=node(label)
        for(direction in listOf(KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_UP)) {
            repeat(16) { if(found==null) { key(direction); found=node(label) } }
        }
        var item=found ?: error("D-pad cannot reach $label")
        if(item.isClickable) return item
        for(i in 0 until item.childCount) item.getChild(i)?.let { child ->
            if(child.isClickable) { item.recycle(); return child }; child.recycle()
        }
        repeat(6) { val parent=item.parent ?: error("No control for $label"); item.recycle(); item=parent; if(item.isClickable) return item }
        error("No button $label")
    }
    fun select(label: String) {
        val target=control(label)
        target.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        Thread.sleep(150)
        check(target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) { "Cannot focus $label" }
        target.recycle(); key(KeyEvent.KEYCODE_DPAD_CENTER); Thread.sleep(500)
    }
    fun shot(name: String) {
        val image=ui.takeScreenshot() ?: error("No screenshot")
        File(test.targetContext.filesDir,"j3c-$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
        image.recycle()
    }
    fun foregroundPackage(): String? = ui.rootInActiveWindow?.let { val name=it.packageName?.toString(); it.recycle(); name }
    fun returnWithBack() {
        repeat(6) { if(foregroundPackage()==test.targetContext.packageName) return; key(KeyEvent.KEYCODE_BACK) }
        check(foregroundPackage()==test.targetContext.packageName) { "Back did not return to RC TV" }
    }
}

object J3CReview {
    fun settings(test: Instrumentation, large: Boolean) {
        val prefs=Preferences(test.targetContext)
        val before=runBlocking { prefs.state.first() }
        val driver=TvReview(test)
        var activity: android.app.Activity?=null
        val evidence=StringBuilder()
        try {
            if(large) runBlocking { prefs.setTextSize("Extra Large") }
            activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1500)
            driver.select("Settings"); driver.select("Apps & Media")
            driver.control("Android app settings").recycle(); driver.shot(if(large) "apps-xl" else "apps")
            val space=readInternalStorage() ?: error("No real storage stats")
            check(space.totalBytes>0 && space.availableBytes in 0..space.totalBytes)
            evidence.append("Internal data space: ${space.availableBytes} available / ${space.totalBytes} total bytes\n")
            fun destination(item: SystemDestination) {
                driver.select(item.label)
                val expected=resolvedSystemSettings(test.targetContext,item.actions)
                    ?: resolvedSystemSettings(test.targetContext, listOf(android.provider.Settings.ACTION_SETTINGS))
                    ?: error("No settings target")
                repeat(20) { if(driver.foregroundPackage()!=expected.component!!.packageName) Thread.sleep(100) }
                check(driver.foregroundPackage()==expected.component!!.packageName) { "${item.label} did not open ${expected.component}" }
                evidence.append("${item.label}: ${expected.component!!.flattenToShortString()}\n")
                driver.shot("destination-${item.key}")
                driver.returnWithBack()
            }
            destination(appSettingsDestination); destination(storageSettingsDestination)
            driver.key(KeyEvent.KEYCODE_BACK); driver.select("System & Recovery")
            driver.shot(if(large) "system-xl" else "system")
            essentialSystemDestinations.forEach { destination(it) }
            driver.key(KeyEvent.KEYCODE_BACK)
            driver.control("System & Recovery").let { check(it.isFocused) { "Category focus not restored" }; it.recycle() }
            check(runBlocking { prefs.state.first() }.let { it.favourites==before.favourites && it.favouriteOrder==before.favouriteOrder })
            File(test.targetContext.filesDir,"j3c-settings-evidence.txt").writeText(evidence.toString())
        } catch(failure: Throwable) { driver.shot("failure"); throw failure }
        finally { test.runOnMainSync { activity?.finish() }; runBlocking { prefs.setTextSize(before.textSize) } }
    }
}

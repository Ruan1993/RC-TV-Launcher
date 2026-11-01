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
        var target=control(label)
        target.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        Thread.sleep(150)
        if (!target.isFocused && !target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
            // A preceding shell tap can leave Compose in touch input mode.
            target.recycle(); key(KeyEvent.KEYCODE_DPAD_UP); target=control(label)
        }
        check(target.isFocused || target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) { "Cannot focus $label" }
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
    fun startupOrdering(test: Instrumentation) {
        val before=runBlocking { Preferences(test.targetContext).state.first() }
        val grants=za.co.rc.tv.link.SecureRecord(test.targetContext,"companion-grants").read()?.optJSONArray("grants")?.toString()
        val keyboard=android.provider.Settings.Secure.getString(test.targetContext.contentResolver,android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
        val driver=TvReview(test)
        var activity: android.app.Activity?=null
        try {
            activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1500)
            val stream=driver.ui.executeShellCommand("logcat -d --pid=${android.os.Process.myPid()} -v threadtime -s RcStartup")
            val trace=android.os.ParcelFileDescriptor.AutoCloseInputStream(stream).bufferedReader().use { it.readText() }
            val draw=trace.indexOf("Home content drawn;")
            val optional=trace.indexOf("Optional resume work started")
            val network=trace.indexOf("Network indicator enabled")
            check(draw>=0 && optional>draw && network>draw) { "Optional work ran before Home content draw: $trace" }
            driver.select("Settings"); driver.select("Screensaver & Power")
            driver.control(before.screensaverProvider).recycle()
            driver.select("Power off & restart help")
            driver.control("Close").let { check(it.isFocused); it.recycle() }
            driver.shot("power-help"); driver.key(KeyEvent.KEYCODE_BACK)
            driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK)
            val after=runBlocking { Preferences(test.targetContext).state.first() }
            check(after.favourites==before.favourites && after.favouriteOrder==before.favouriteOrder && after.screensaverProvider==before.screensaverProvider)
            check(za.co.rc.tv.link.SecureRecord(test.targetContext,"companion-grants").read()?.optJSONArray("grants")?.toString()==grants)
            check(android.provider.Settings.Secure.getString(test.targetContext.contentResolver,android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)==keyboard)
            File(test.targetContext.filesDir,"j3c-startup-order.txt").writeText(trace+"\nHome navigation, power help and all ${after.favourites.size} favourites/order/provider, logical pairing grants and keyboard preserved.\n")
        } catch(failure: Throwable) { driver.shot("startup-failure"); throw failure }
        finally { test.runOnMainSync { activity?.finish() } }
    }
    fun largeTextAndStorageStats(test: Instrumentation) {
        val prefs=Preferences(test.targetContext)
        val before=runBlocking { prefs.state.first() }
        val driver=TvReview(test)
        var activity: android.app.Activity?=null
        try {
            runBlocking { prefs.setTextSize("Extra Large") }
            activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1500)
            driver.select("Settings"); driver.select("Apps & Media"); driver.control("Storage & USB settings").recycle(); driver.shot("apps-xl")
            driver.key(KeyEvent.KEYCODE_BACK); driver.select("System & Recovery"); driver.select("Launcher Management"); driver.shot("launcher-xl")
            driver.select("Reset RC TV Appearance"); driver.shot("reset-confirmation-xl"); driver.select("Cancel")
            driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK)
            driver.select("Screensaver & Power"); driver.select(before.screensaverProvider); driver.shot("provider-choice-xl"); driver.key(KeyEvent.KEYCODE_BACK)
            val stats=test.targetContext.getSystemService(android.app.usage.StorageStatsManager::class.java)
                .queryStatsForPackage(android.os.storage.StorageManager.UUID_DEFAULT,test.targetContext.packageName,android.os.Process.myUserHandle())
            File(test.targetContext.filesDir,"j3c-size-evidence.txt").writeText("StorageStats own package: appBytes=${stats.appBytes}; dataBytes=${stats.dataBytes}; cacheBytes=${stats.cacheBytes}\n")
            check(runBlocking { prefs.state.first() }.let { it.favourites==before.favourites && it.favouriteOrder==before.favouriteOrder && it.screensaverProvider==before.screensaverProvider })
        } catch(failure: Throwable) { driver.shot("failure"); throw failure }
        finally { test.runOnMainSync { activity?.finish() }; runBlocking { prefs.setTextSize(before.textSize) } }
    }
    /** Exercise the real confirmation, then restore only its visual-key allowlist. */
    @Suppress("UNCHECKED_CAST")
    fun confirmedReset(test: Instrumentation) {
        val prefs=Preferences(test.targetContext)
        val field=Preferences::class.java.getDeclaredField("store").apply { isAccessible=true }
        val store=field.get(prefs) as androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
        val snapshot=runBlocking { store.data.first() }
        val before=runBlocking { prefs.state.first() }
        val resetCopy=snapshot.toMutablePreferences().also { resetLauncherVisualPreferences(it) }
        val visualKeys=snapshot.asMap().keys-resetCopy.asMap().keys
        val pairing=za.co.rc.tv.link.SecureRecord(test.targetContext,"companion-grants").read()?.optJSONArray("grants")?.toString()
        val keyboard=android.provider.Settings.Secure.getString(test.targetContext.contentResolver,android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
        val driver=TvReview(test)
        var activity: android.app.Activity?=null
        try {
            activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1500)
            driver.select("Settings"); driver.select("System & Recovery"); driver.select("Launcher Management")
            driver.select("Reset RC TV Appearance"); driver.select("Reset appearance")
            val reset=runBlocking { kotlinx.coroutines.withTimeout(5000) { store.data.first { data -> data.asMap().keys.none { it in visualKeys } } }; prefs.state.first() }
            check(reset.theme=="Cinematic" && reset.wallpaper=="mountain" && reset.textSize=="Standard" && reset.accent=="Original (Default)")
            check(reset.favourites==before.favourites && reset.favouriteOrder==before.favouriteOrder && reset.reducedMotion==before.reducedMotion)
            check(reset.screensaverProvider==before.screensaverProvider && reset.screensaverStyle==before.screensaverStyle && reset.screensaverMinutes==before.screensaverMinutes)
            check(za.co.rc.tv.link.SecureRecord(test.targetContext,"companion-grants").read()?.optJSONArray("grants")?.toString()==pairing)
            check(android.provider.Settings.Secure.getString(test.targetContext.contentResolver,android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)==keyboard)
            driver.shot("reset-applied-test")
        } finally {
            runBlocking { store.updateData { current -> current.toMutablePreferences().apply {
                resetLauncherVisualPreferences(this)
                snapshot.asMap().filterKeys { it in visualKeys }.forEach { (key,value) -> this[key as androidx.datastore.preferences.core.Preferences.Key<Any>] = value }
            } } }
            test.runOnMainSync { activity?.finish() }
        }
        val restored=runBlocking { prefs.state.first() }
        check(restored.accent==before.accent && restored.fontStyle==before.fontStyle && restored.textSize==before.textSize && restored.focusAnimation==before.focusAnimation)
        check(restored.favourites==before.favourites && restored.favouriteOrder==before.favouriteOrder)
        File(test.targetContext.filesDir,"j3c-reset-evidence.txt").writeText("Confirmed production reset applied default visuals; all ${before.favourites.size} favourites/order, logical pairing grants, keyboard and screensaver choices preserved. Original visual keys restored after test.\n")
    }
    fun launcher(test: Instrumentation) {
        val prefs=Preferences(test.targetContext)
        val before=runBlocking { prefs.state.first() }
        val driver=TvReview(test)
        var activity: android.app.Activity?=null
        try {
            activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(1500)
            driver.select("Settings"); driver.select("System & Recovery"); driver.select("Launcher Management")
            driver.control("Set RC TV as Default Home").recycle(); driver.shot("launcher-management")
            val home=HomeIntegration.inspect(test.targetContext)
            File(test.targetContext.filesDir,"j3c-home-before.txt").writeText("${home.resolvedPackage}\n${home.description(test.targetContext.packageName)}\nHome settings=${home.homeSettingsAvailable}; role available=${home.roleAvailable}; held=${home.roleHeld}\n")
            driver.select("Reset RC TV Appearance")
            driver.control("Cancel").let { check(it.isFocused) { "Reset should focus Cancel" }; it.recycle() }
            driver.shot("reset-confirmation"); driver.key(KeyEvent.KEYCODE_BACK)
            val cancelled=runBlocking { prefs.state.first() }
            check(cancelled.copy(wallpaper=before.wallpaper, rotationAt=before.rotationAt)==before &&
                (before.rotationEnabled || cancelled.wallpaper==before.wallpaper)) { "Cancelling reset changed preferences" }
            driver.select("Open Android Settings"); check(driver.foregroundPackage()?.contains("settings")==true); driver.returnWithBack()
            driver.key(KeyEvent.KEYCODE_BACK)
            driver.control("Launcher Management").let { check(it.isFocused) { "Nested Back did not restore Launcher Management focus" }; it.recycle() }
            driver.select("Launcher Management")
            driver.select("Switch to Original Xiaomi Home")
            check(driver.foregroundPackage()=="com.google.android.permissioncontroller" || driver.foregroundPackage()?.contains("settings")==true)
            driver.shot("home-selection")
            driver.returnWithBack()
            check(HomeIntegration.inspect(test.targetContext).resolvedPackage==home.resolvedPackage) { "Opening chooser changed Home without selection" }
            check(runBlocking { prefs.state.first() }.let { it.favourites==before.favourites && it.favouriteOrder==before.favouriteOrder })
            File(test.targetContext.filesDir,"j3c-launcher-evidence.txt").writeText("Launcher UI, reset confirmation/Cancel, Android settings, nested Back focus and Home selection entry passed; no default changed\n")
        } catch(failure: Throwable) { driver.shot("failure"); throw failure }
        finally { test.runOnMainSync { activity?.finish() } }
    }
    fun providers(test: Instrumentation) {
        val prefs=Preferences(test.targetContext)
        val before=runBlocking { prefs.state.first() }
        val driver=TvReview(test)
        var activity: MainActivity?=null
        try {
            runBlocking { prefs.setScreensaverMinutes(10) }
            activity=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            Thread.sleep(1500)
            driver.select("Settings"); driver.select("Screensaver & Power")
            driver.select(before.screensaverProvider); driver.select(ANDROID_SCREENSAVER)
            runBlocking { kotlinx.coroutines.withTimeout(5000) { prefs.state.first { it.screensaverProvider==ANDROID_SCREENSAVER } } }
            driver.shot("provider-android")
            driver.select("Android ambient mode settings")
            check(driver.foregroundPackage()?.contains("settings")==true)
            driver.returnWithBack()
            driver.select("Preview Screensaver")
            check(driver.node("RC TV screensaver")!=null)
            driver.key(KeyEvent.KEYCODE_BACK)
            check(runBlocking { prefs.state.first() }.screensaverProvider==ANDROID_SCREENSAVER)
            driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK)
            fun expire() { test.runOnMainSync {
                val field=MainActivity::class.java.getDeclaredField("lastInteraction\$delegate").apply { isAccessible=true }
                (field.get(activity) as androidx.compose.runtime.MutableLongState).longValue=SystemClock.elapsedRealtime()-599_000L
            } }
            expire(); Thread.sleep(1800); check(driver.node("RC TV screensaver")==null) { "Android provider left RC timer active" }
            driver.select("Settings"); driver.select("Screensaver & Power")
            driver.select(ANDROID_SCREENSAVER); driver.select(RC_SCREENSAVER)
            runBlocking { kotlinx.coroutines.withTimeout(5000) { prefs.state.first { it.screensaverProvider==RC_SCREENSAVER } } }
            driver.shot("provider-rc")
            driver.key(KeyEvent.KEYCODE_BACK); driver.key(KeyEvent.KEYCODE_BACK)
            expire(); Thread.sleep(1800); check(driver.node("RC TV screensaver")!=null) { "RC provider failed to activate" }
            driver.key(KeyEvent.KEYCODE_DPAD_UP)
            check(runBlocking { prefs.state.first() }.screensaverStyle==before.screensaverStyle)
        } catch(failure: Throwable) { driver.shot("failure"); throw failure }
        finally {
            test.runOnMainSync { activity?.finish() }
            runBlocking { prefs.setScreensaverProvider(before.screensaverProvider); prefs.setScreensaverMinutes(before.screensaverMinutes) }
        }
        val after=runBlocking { prefs.state.first() }
        check(after.favourites==before.favourites && after.favouriteOrder==before.favouriteOrder)
        File(test.targetContext.filesDir,"j3c-provider-persist.json").writeText(org.json.JSONObject()
            .put("provider", after.screensaverProvider).put("minutes",after.screensaverMinutes).put("style",after.screensaverStyle)
            .put("favourites", org.json.JSONArray(after.favourites.sorted())).put("order",org.json.JSONArray(after.favouriteOrder)).toString())
        File(test.targetContext.filesDir,"j3c-provider-evidence.txt").writeText("Upgrade provider: ${before.screensaverProvider}\nAndroid auto off; explicit RC preview works\nRC auto on\nStyle/interval and all ${after.favourites.size} favourites/order preserved\n")
    }
    fun providerPersistence(test: Instrumentation) {
        val saved=org.json.JSONObject(File(test.targetContext.filesDir,"j3c-provider-persist.json").readText())
        val prefs=runBlocking { Preferences(test.targetContext).state.first() }
        check(prefs.screensaverProvider==saved.getString("provider") && prefs.screensaverMinutes==saved.getInt("minutes") && prefs.screensaverStyle==saved.getString("style"))
        check(org.json.JSONArray(prefs.favourites.sorted()).toString()==saved.getJSONArray("favourites").toString())
        check(org.json.JSONArray(prefs.favouriteOrder).toString()==saved.getJSONArray("order").toString())
        File(test.targetContext.filesDir,"j3c-provider-evidence.txt").appendText("Provider/interval/style/favourites/order persisted across process restart\n")
    }
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

package za.co.rc.tv

import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File

/** Bounded smoke test; does not change Android Home, services, keyboard or package states. */
object SprintUiCheck {
    fun run(test: Instrumentation) {
        val setup = test.targetContext.getSharedPreferences("easy-setup", 0)
        val seen = setup.getBoolean("seen", false)
        val session = za.co.rc.tv.core.SetupSession(test.targetContext)
        val active = session.active
        val originalStep = session.step
        setup.edit().putBoolean("seen", true).commit(); session.move(za.co.rc.tv.core.SetupStep.WELCOME); session.pause()
        val activity = test.startActivitySync(Intent(test.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val ui = test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun find(text: String): AccessibilityNodeInfo? {
            ui.clearCache()
            fun walk(item: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (item.text?.toString() == text) return AccessibilityNodeInfo.obtain(item)
                for (i in 0 until item.childCount) item.getChild(i)?.let { child ->
                    val found = walk(child); child.recycle(); if (found != null) return found
                }
                return null
            }
            val root = ui.rootInActiveWindow ?: return null
            return walk(root).also { root.recycle() }
        }
        fun click(text: String) {
            var node: AccessibilityNodeInfo? = null
            repeat(30) { if (node == null) { node = find(text); if (node == null) Thread.sleep(100) } }
            var current = checkNotNull(node) { "Missing: $text" }
            repeat(5) { if (!current.isClickable) current.parent?.let { parent -> current.recycle(); current = parent } }
            check(current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Cannot click: $text" }
            current.recycle(); Thread.sleep(250)
        }
        fun shot(name: String) {
            val bitmap = checkNotNull(ui.takeScreenshot())
            File(test.targetContext.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        try {
            click("Settings"); shot("sprint-settings.png")
            click("System & Recovery"); click("Easy setup"); shot("sprint-setup.png")
            // Physical-key injection here is emulator input, not a TV acceptance claim.
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
            Thread.sleep(250); shot("sprint-setup-next.png")
            check(find("2 OF 10  ·  Make it yours") != null) { "D-pad Continue did not enter Appearance" }
            test.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            Thread.sleep(250); shot("sprint-setup-back.png")
            check(find("1 OF 10  ·  Welcome to RC TV") != null) { "Physical Back did not return to Welcome" }
        } finally {
            test.runOnMainSync { activity.finish() }
            setup.edit().putBoolean("seen", seen).commit()
            session.move(originalStep); session.pause(); if (active) session.begin()
        }
    }
}

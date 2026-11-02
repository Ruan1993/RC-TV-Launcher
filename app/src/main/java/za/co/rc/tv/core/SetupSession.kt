package za.co.rc.tv.core

import android.content.Context
import android.content.Intent

enum class SetupStep(val title: String) {
    WELCOME("Welcome to RC TV"), APPEARANCE("Make it yours"), FAVOURITES("Your favourite apps"),
    FEATURES("Choose optional features"), PERMISSIONS("Permissions & keyboard"), PHONE("Connect your phone"),
    HOME("Choose your Home screen"), VERIFY("Test the Home button"), RECOVERY("Recovery readiness"), COMPLETE("Ready to watch")
}

object SetupVerification {
    fun homeVerified(resolvesToRc: Boolean, nativeHomeObserved: Boolean, interceptionEnabled: Boolean) =
        resolvesToRc && nativeHomeObserved && !interceptionEnabled
    fun nativeHome(action: String?, categories: Set<String>?) =
        action == "android.intent.action.MAIN" && categories?.contains("android.intent.category.HOME") == true
    fun restored(stockEnabled: Boolean, resolvesToStock: Boolean, userConfirmed: Boolean, interceptionEnabled: Boolean) =
        stockEnabled && resolvesToStock && userConfirmed && !interceptionEnabled
}

/** Small synchronous checkpoints: acknowledgement follows a durable write, never Activity saved state alone. */
class SetupSession(context: Context, storeName: String = "setup-session-v1") {
    private val prefs = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
    val active get() = prefs.getBoolean("active", false)
    val step get() = runCatching { SetupStep.valueOf(prefs.getString("step", null) ?: "WELCOME") }.getOrDefault(SetupStep.WELCOME)
    val phone get() = prefs.getBoolean("phone", false)
    val otherApps get() = prefs.getBoolean("other-apps", false)
    val typing get() = prefs.getBoolean("typing", false)
    val homeObserved get() = prefs.getBoolean("home-observed", false)
    fun begin() = save(prefs.edit().putBoolean("active", true))
    fun move(step: SetupStep, skipped: Boolean = false) = save(prefs.edit().putString("step", step.name)
        .putBoolean("active", true).putBoolean("skipped-${this.step.name}", skipped))
    fun features(phone: Boolean, otherApps: Boolean, typing: Boolean) = save(prefs.edit()
        .putBoolean("phone", phone).putBoolean("other-apps", otherApps).putBoolean("typing", typing))
    fun pause() = save(prefs.edit().putBoolean("active", false))
    fun complete() = save(prefs.edit().putBoolean("active", false).putString("step", "WELCOME").putBoolean("home-test", false))
    fun skipped() = SetupStep.entries.filter { prefs.getBoolean("skipped-${it.name}", false) }.map { it.title }
    fun startHomeTest() = save(prefs.edit().putBoolean("home-test", true).putBoolean("home-observed", false).putLong("test-started", System.currentTimeMillis()))
    fun cancelHomeTest() = save(prefs.edit().putBoolean("home-test", false))
    fun observeHome(intent: Intent) {
        val age = System.currentTimeMillis() - prefs.getLong("test-started", 0)
        if (prefs.getBoolean("home-test", false) && age in 0..120_000 && SetupVerification.nativeHome(intent.action, intent.categories))
            save(prefs.edit().putBoolean("home-observed", true).putBoolean("home-test", false))
    }
    private fun save(editor: android.content.SharedPreferences.Editor) { check(editor.commit()) { "Could not save setup progress. Try again." } }
}

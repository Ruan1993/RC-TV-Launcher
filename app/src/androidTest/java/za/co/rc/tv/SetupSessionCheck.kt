package za.co.rc.tv

import android.app.Instrumentation
import android.content.Intent
import za.co.rc.tv.core.*

/** Test-APK store only: no launchers, system permissions or live pairing state are changed. */
object SetupSessionCheck {
    fun run(test: Instrumentation) {
        val context = test.targetContext
        val store = "setup-session-instrumentation"
        check(context.getSharedPreferences(store, 0).edit().clear().commit())
        val first = SetupSession(context, store)
        first.begin(); first.features(true, false, true); first.move(SetupStep.PERMISSIONS)
        val recreated = SetupSession(context, store)
        check(recreated.active && recreated.step == SetupStep.PERMISSIONS && recreated.phone && recreated.typing && !recreated.otherApps)
        recreated.pause()
        check(!SetupSession(context, store).active && SetupSession(context, store).step == SetupStep.PERMISSIONS)
        recreated.startHomeTest()
        recreated.observeHome(Intent("za.co.rc.tv.EXPERIMENT_HOME"))
        check(!SetupSession(context, store).homeObserved)
        recreated.observeHome(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        check(SetupSession(context, store).homeObserved)
        recreated.complete()
        check(!SetupSession(context, store).active && SetupSession(context, store).step == SetupStep.WELCOME)
        context.getSharedPreferences(store, 0).edit().clear().commit()
    }
}

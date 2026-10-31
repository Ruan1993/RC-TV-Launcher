package za.co.rc.tv.core

object SetupProgress {
    fun offerOnFirstOpen(seen: Boolean, existingPreferences: Boolean) = !seen && !existingPreferences
    fun automaticStep(step: Int, actualHomeMatches: Boolean, appPermission: Boolean, serviceConnected: Boolean): Int = when {
        step == 1 && actualHomeMatches -> 2
        step == 2 && appPermission && serviceConnected -> 3
        else -> step
    }
}

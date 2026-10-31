package za.co.rc.tv.core

val focusAnimations = listOf("Off", "Subtle", "Standard", "Enhanced")
fun legacyFocusAnimation(motion: String?) = when(motion) { "Minimal" -> "Off"; "Balanced" -> "Subtle"; else -> "Standard" }
fun validFocusAnimation(value: String?) = value?.takeIf { it in focusAnimations } ?: "Standard"
fun activeFocusAnimation(prefs: LauncherPreferences) = if(prefs.reducedMotion) "Off" else validFocusAnimation(prefs.focusAnimation)
fun focusScale(intensity: String) = when(intensity) { "Subtle" -> 1.025f; "Standard" -> 1.05f; "Enhanced" -> 1.10f; else -> 1f }

package za.co.rc.tv.companion

/** Keep permission and security decisions under local physical-remote control. */
object RemotePolicy {
    fun localOnly(packageName: String, ownPackage: String) = packageName.isBlank() || packageName == ownPackage ||
        packageName.contains("permissioncontroller") || packageName.contains("packageinstaller") ||
        packageName == "com.android.systemui" || packageName == "android" ||
        packageName.endsWith(".settings") || packageName.contains("tv.settings")
    fun validPayload(key: String, text: String) = key in za.co.rc.tv.link.LinkWire.COMMANDS && text.length <= 160 &&
        (key in setOf("SEARCH_TEXT", "INPUT_TEXT") || text.isEmpty())
}

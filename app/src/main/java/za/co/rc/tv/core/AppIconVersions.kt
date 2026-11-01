package za.co.rc.tv.core

/** Versions prevent a decode already in flight from publishing an icon for a changed package. */
internal class AppIconVersions {
    private val versions = java.util.concurrent.ConcurrentHashMap<String, Long>()
    fun key(packageName: String, pixels: Int) = "$packageName@$pixels#${versions[packageName] ?: 0L}"
    fun invalidate(packageName: String) { versions.compute(packageName) { _, old -> (old ?: 0L) + 1 } }
}

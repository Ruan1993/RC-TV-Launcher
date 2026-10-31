package za.co.rc.tv.core

import za.co.rc.tv.BuildConfig
import java.util.concurrent.atomic.AtomicInteger

/** Counters are active only in the shell-profileable, non-debuggable profile build. */
object RenderDiagnostics {
    private val wallpaper = AtomicInteger()
    private val shell = AtomicInteger()
    private val tiles = AtomicInteger()
    private val decode = AtomicInteger()
    private val icons = AtomicInteger()
    fun wallpaperComposed() { if (BuildConfig.PROFILE_DIAGNOSTICS) wallpaper.incrementAndGet() }
    fun shellComposed() { if (BuildConfig.PROFILE_DIAGNOSTICS) shell.incrementAndGet() }
    fun tileComposed() { if (BuildConfig.PROFILE_DIAGNOSTICS) tiles.incrementAndGet() }
    fun wallpaperDecoded() { if (BuildConfig.PROFILE_DIAGNOSTICS) decode.incrementAndGet() }
    fun iconRasterized() { if (BuildConfig.PROFILE_DIAGNOSTICS) icons.incrementAndGet() }
    fun snapshot() {
        if (BuildConfig.PROFILE_DIAGNOSTICS) android.util.Log.i("RcProfile",
            "wallpaper=${wallpaper.get()} shell=${shell.get()} tiles=${tiles.get()} decodes=${decode.get()} icons=${icons.get()}")
    }
}

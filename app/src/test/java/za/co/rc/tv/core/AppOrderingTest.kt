package za.co.rc.tv.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderingTest {
    @Test fun preferredAppsAreOrderedAndDuplicatePackagesRemoved() {
        val apps = listOf(LauncherApp("z", "Zebra"), LauncherApp("yt", "YouTube"), LauncherApp("music", "YouTube Music"), LauncherApp("yt", "YouTube"), LauncherApp("f1", "F1 TV"), LauncherApp("cine", "YouCine"))
        assertEquals(listOf("cine", "music", "yt", "f1", "z"), AppOrdering.sort(apps).map { it.packageName })
    }
    @Test fun absentPreferredAppsAreNeverInvented() { assertEquals(emptyList<LauncherApp>(), AppOrdering.sort(emptyList())) }
}

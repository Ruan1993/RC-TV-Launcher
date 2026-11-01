package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class AppIconVersionsTest {
    @Test fun warmRequestsKeepTheSameKeyWithoutPackageChanges() {
        val versions = AppIconVersions()
        assertEquals(versions.key("app.one", 80), versions.key("app.one", 80))
        assertNotEquals(versions.key("app.one", 80), versions.key("app.one", 48))
    }
    @Test fun changesInvalidateOnlyTheAffectedPackageAndAllItsSizes() {
        val versions = AppIconVersions()
        val before = versions.key("app.one", 80)
        val otherSize = versions.key("app.one", 48)
        val untouched = versions.key("app.two", 80)
        versions.invalidate("app.one")
        assertNotEquals(before, versions.key("app.one", 80))
        assertNotEquals(otherSize, versions.key("app.one", 48))
        assertEquals(untouched, versions.key("app.two", 80))
        val changed = versions.key("app.one", 80)
        versions.invalidate("app.one")
        assertNotEquals(changed, versions.key("app.one", 80))
    }
}

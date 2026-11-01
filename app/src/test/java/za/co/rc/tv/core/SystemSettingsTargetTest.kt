package za.co.rc.tv.core
import org.junit.Assert.*
import org.junit.Test
class SystemSettingsTargetTest {
    @Test fun rejectStubsUnexportedAndNonSystemHandlers() {
        assertFalse(usableSystemSettingsTarget("com.google.android.tv.frameworkpackagestubs", "Stubs.SettingsStub", true, true))
        assertFalse(usableSystemSettingsTarget("android", "com.android.internal.app.ResolverActivity", true, true))
        assertFalse(usableSystemSettingsTarget("settings", "EmptyStubActivity", true, true))
        assertFalse(usableSystemSettingsTarget("settings", "StorageActivity", false, true))
        assertFalse(usableSystemSettingsTarget("other", "StorageActivity", true, false))
        assertTrue(usableSystemSettingsTarget("settings", "StorageActivity", true, true))
    }
}

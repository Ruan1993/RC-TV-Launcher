package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test
import za.co.rc.tv.companion.RemotePolicy

class RemotePolicyTest {
    @Test fun permissionAndSettingsInterfacesRemainLocal() {
        listOf("", "za.co.rc.tv", "com.android.tv.settings", "com.android.settings", "com.google.android.permissioncontroller",
            "com.android.packageinstaller", "com.android.systemui", "android").forEach {
            assertTrue(it, RemotePolicy.localOnly(it, "za.co.rc.tv"))
        }
        assertFalse(RemotePolicy.localOnly("com.google.android.youtube.tv", "za.co.rc.tv"))
    }
    @Test fun textAndControlPayloadsAreBoundedAndAllowlisted() {
        assertTrue(RemotePolicy.validPayload("INPUT_TEXT", "a".repeat(160)))
        assertFalse(RemotePolicy.validPayload("INPUT_TEXT", "a".repeat(161)))
        assertTrue(RemotePolicy.validPayload("MEDIA_PLAY_PAUSE", ""))
        assertTrue(RemotePolicy.validPayload("CLOSE_SETUP", ""))
        assertTrue(RemotePolicy.validPayload("INPUT_DELETE", ""))
        assertFalse(RemotePolicy.validPayload("INPUT_DELETE", "unexpected"))
        assertFalse(RemotePolicy.validPayload("CLOSE_SETUP", "unexpected"))
        assertFalse(RemotePolicy.validPayload("HOME", "unexpected"))
        assertFalse(RemotePolicy.validPayload("SHELL", ""))
        assertFalse(RemotePolicy.validPayload("ASSIST_PROMPT", "hello"))
    }
}

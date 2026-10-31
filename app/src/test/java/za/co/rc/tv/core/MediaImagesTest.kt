package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class MediaImagesTest {
    @Test fun requestsOnlyVersionAppropriateImageReadAccess() {
        assertArrayEquals(arrayOf("android.permission.READ_EXTERNAL_STORAGE"), imageReadPermissions(30))
        assertArrayEquals(arrayOf("android.permission.READ_MEDIA_IMAGES"), imageReadPermissions(33))
        assertArrayEquals(arrayOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"), imageReadPermissions(34))
    }
}

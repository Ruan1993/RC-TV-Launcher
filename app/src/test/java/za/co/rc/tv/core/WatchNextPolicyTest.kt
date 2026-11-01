package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class WatchNextPolicyTest {
    private fun item(type: Int? = 0, browsable: Boolean = true, position: Long? = null, duration: Long? = null) =
        continueItem(1,"org.videolan.vlc","A real title","vlc://content/1",null,type,browsable,position,duration)

    @Test fun recommendationsWatchlistsAndHiddenContentAreNotContinueWatching() {
        assertNotNull(item())
        for (type in listOf(null,1,2,3,99)) assertNull(item(type))
        assertNull(item(browsable=false))
    }
    @Test fun progressIsShownOnlyForConsistentProviderMeasurements() {
        assertNull(item()!!.progress)
        assertNull(item(position=10,duration=0)!!.progress)
        assertNull(item(position=-1,duration=100)!!.progress)
        assertNull(item(position=101,duration=100)!!.progress)
        assertEquals(.5f,item(position=50,duration=100)!!.progress!!,0f)
        assertEquals(0f,item(position=0,duration=100)!!.progress!!,0f)
        assertEquals(1f,item(position=Long.MAX_VALUE,duration=Long.MAX_VALUE)!!.progress!!,0f)
    }
    @Test fun unusableEntriesAreSkippedAndRealTitlesAreSanitised() {
        assertNull(continueItem(-1,"valid.app","Title","https://example.com",null,0,true,null,null))
        assertNull(continueItem(1,"bad/package","Title","https://example.com",null,0,true,null,null))
        assertNull(continueItem(1,"valid.app"," ","https://example.com",null,0,true,null,null))
        assertNull(continueItem(1,"valid.app","Title",null,null,0,true,null,null))
        assertEquals("Real title",continueItem(1,"valid.app","  Real title\n ","https://example.com",null,0,true,null,null)!!.title)
    }
    @Test fun artworkNeverOpensRawFilesCleartextHttpOrEmbeddedCredentials() {
        for (uri in listOf(null,"file:///sdcard/private.jpg","http://example.com/image.jpg","https://user:password@example.com/image.jpg","not a URI")) assertNull(safeWatchNextArtwork(uri))
        assertEquals("https://example.com/image.jpg",safeWatchNextArtwork("https://example.com/image.jpg"))
        assertEquals("content://org.videolan.vlc/image/1",safeWatchNextArtwork("content://org.videolan.vlc/image/1"))
        assertTrue(LauncherPreferences().continueWatching)
    }
}

package za.co.rc.tv.core
import org.junit.Assert.*
import org.junit.Test
class NetworkStatusTest {
    @Test fun actualTransportTakesPriorityAndMissingInformationIsNeutral() {
        assertEquals(NetworkKind.WIFI,classifyNetwork(true,false,false,true,-60).kind)
        assertEquals(NetworkKind.ETHERNET,classifyNetwork(false,true,true,false,null).kind)
        assertEquals(NetworkKind.OTHER,classifyNetwork(false,false,true,true,null).kind)
        assertEquals(NetworkKind.WIFI_OFF,classifyNetwork(false,false,false,false,null).kind)
        assertEquals(NetworkKind.WIFI_DISCONNECTED,classifyNetwork(false,false,false,true,null).kind)
        assertEquals(NetworkKind.UNKNOWN,classifyNetwork(false,false,false,null,null).kind)
    }
    @Test fun invalidRssiNeverBecomesInventedSignalStrength() {
        assertNull(wifiLevel(null)); assertNull(wifiLevel(-127)); assertNull(wifiLevel(0)); assertNull(wifiLevel(200))
        assertEquals(0,wifiLevel(-100)); assertEquals(4,wifiLevel(-40)); assertEquals(3,wifiLevel(-60))
        assertFalse(classifyNetwork(true,false,false,true,-60,false).internet)
    }
}

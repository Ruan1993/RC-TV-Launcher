package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test
import za.co.rc.tv.companion.PairingGate

class PairingGateTest {
    @Test fun approvalReplayRevocationAndDisconnect() {
        var now = 1000L; val gate = PairingGate { now }; val code = gate.start()
        assertFalse(gate.authenticated("unauthorised"))
        val request = gate.request(code, "Test phone")!!
        assertEquals(false to null, gate.poll(request))
        assertTrue(gate.approve(request)); val token = gate.poll(request)!!.second!!
        assertTrue(gate.authorize(token, 1)); now += 50
        assertFalse(gate.authorize(token, 1)); assertTrue(gate.authorize(token, 2))
        gate.revoke(gate.sessions().single().id); assertFalse(gate.authenticated(token))
        val next = gate.request(gate.start(), "Second")!!; gate.approve(next)
        val second = gate.poll(next)!!.second!!; gate.disconnect(second); assertFalse(gate.authenticated(second))
    }
    @Test fun expiryRateLimitAndStopInvalidateRequests() {
        var now = 0L; val gate = PairingGate { now }; val code = gate.start()
        repeat(5) { assertNull(gate.request("bad", "Test")) }; assertNull(gate.request(code, "Test"))
        now = 60_001; val request = gate.request(code, "Test")!!
        now += 60_001; assertNull(gate.poll(request)); assertNull(gate.request(code, "Test"))
        val id = gate.request(gate.start(), "Test")!!; gate.approve(id); val token = gate.poll(id)!!.second!!
        now += 30 * 60_000; assertFalse(gate.authenticated(token))
        gate.start(); assertNull(gate.poll(id)); assertTrue(gate.sessions().isEmpty())
    }
    @Test fun rejectionAndPendingLimit() {
        val gate = PairingGate(); val code = gate.start(); val id = gate.request(code, "Phone")!!
        assertNull(gate.request(code, "Other")); gate.reject(id); assertNull(gate.poll(id)); assertNotNull(gate.request(code, "Other"))
    }
}

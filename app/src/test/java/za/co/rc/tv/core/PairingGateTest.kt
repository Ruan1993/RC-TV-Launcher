package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test
import za.co.rc.tv.companion.PairingGate

class PairingGateTest {
    @Test fun reopeningPairingRenewsExpiredCodeWithoutRevokingApprovedPhone() {
        var now = 1000L; val gate = PairingGate { now }
        val request = gate.request(gate.start(), "Existing phone")!!; gate.approve(request)
        val token = gate.poll(request)!!.second!!
        gate.suspendLeases(); assertEquals("", gate.currentCode())
        gate.preparePairing(); assertTrue(gate.currentCode().matches(Regex("[0-9]{6}")))
        val pending = gate.request(gate.currentCode(), "New phone")!!
        gate.preparePairing(); assertNotNull(gate.poll(pending))
        now += 120_001; assertEquals("", gate.currentCode())
        gate.preparePairing(); assertTrue(gate.currentCode().matches(Regex("[0-9]{6}")))
        assertTrue(gate.authenticated(token))
        assertNotNull(gate.request(gate.currentCode(), "Retry phone"))
    }
    @Test fun approvalReplayRevocationAndDisconnect() {
        var now = 1000L; val gate = PairingGate { now }; val code = gate.start()
        assertFalse(gate.authenticated("unauthorised"))
        val request = gate.request(code, "Test phone")!!
        assertEquals(false to null, gate.poll(request))
        assertTrue(gate.approve(request)); val token = gate.poll(request)!!.second!!; val lease = gate.resume(token)!!
        assertTrue(gate.authorize(token, 1, lease)); now += 50
        assertFalse(gate.authorize(token, 1, lease)); assertTrue(gate.authorize(token, 2, lease))
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
        now += 30 * 24 * 60 * 60_000L; assertTrue(gate.authenticated(token))
        gate.start(); assertNull(gate.poll(id)); assertTrue(gate.sessions().isEmpty())
    }
    @Test fun legacyMigrationAndLeaseExpiryKeepApprovalButRejectExpiredRecords() {
        var now = 1000L; val gate = PairingGate { now }
        val id = gate.request(gate.start(), "Phone")!!; gate.approve(id)
        val token = gate.poll(id)!!.second!!
        val record = gate.records().single().copy(until = now + 60_000)
        gate.restore(listOf(record)); assertEquals(Long.MAX_VALUE, gate.records().single().until)
        val lease = gate.resume(token)!!; assertTrue(gate.validLease(token, lease))
        now += 30 * 60_000L
        assertFalse(gate.validLease(token, lease)); assertFalse(gate.authorize(token, 1, lease))
        assertTrue(gate.authenticated(token)); val fresh = gate.resume(token)!!
        assertNotEquals(lease, fresh); assertTrue(gate.authorize(token, 1, fresh))
        gate.restore(listOf(record)); assertTrue(gate.records().isEmpty()); assertNull(gate.resume(token))
    }
    @Test fun rejectionAndPendingLimit() {
        val gate = PairingGate(); val code = gate.start(); val id = gate.request(code, "Phone")!!
        assertNull(gate.request(code, "Other")); gate.reject(id); assertNull(gate.poll(id)); assertNotNull(gate.request(code, "Other"))
    }
    @Test fun persistedGrantResumesButOldLeaseNeverDoes() {
        val gate = PairingGate(); val id = gate.request(gate.start(), "Phone")!!; gate.approve(id)
        val token = gate.poll(id)!!.second!!; val old = gate.resume(token)!!
        assertTrue(gate.authorize(token, 1, old)); val records = gate.records(); gate.suspendLeases()
        assertFalse(gate.authorize(token, 2, old))
        val restored = PairingGate(); restored.restore(records)
        assertTrue(restored.authenticated(token)); assertFalse(restored.authorize(token, 2, old))
        val fresh = restored.resume(token)!!; assertNotEquals(old, fresh)
        assertTrue(restored.authorize(token, 1, fresh)); restored.revoke(restored.sessions().single().id)
        assertTrue(restored.records().isEmpty()); assertNull(restored.resume(token))
    }
}

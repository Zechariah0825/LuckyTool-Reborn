package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserHomeRouteGateTest {
    @Test fun `repeat activation and back do not redirect again`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        var loads = 0
        assertTrue(gate.openOnce(home) { loads++; true })
        assertFalse(gate.openOnce(home) { loads++; true })
        assertEquals(1, loads)
        assertTrue(gate.openOnce(Any()) { loads++; true })
        assertEquals(2, loads)
    }
    @Test fun `failed activation can retry after native lifecycle becomes ready`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        assertFalse(gate.openOnce(home) { false })
        assertTrue(gate.openOnce(home) { true })
    }
    @Test fun `explicit home click rearms only its own fragment once`() {
        val gate = BrowserHomeRouteGate()
        val first = Any()
        val other = Any()
        assertTrue(gate.openOnce(first) { true })
        assertTrue(gate.openOnce(other) { true })
        gate.rearm(first)
        var loads = 0
        assertTrue(gate.openOnce(first) { loads++; true })
        assertFalse(gate.openOnce(first) { loads++; true })
        assertFalse(gate.openOnce(other) { loads++; true })
        assertEquals(1, loads)
    }
    @Test fun `synchronous nested lifecycle callback cannot navigate twice`() {
        val gate = BrowserHomeRouteGate()
        val fragment = Any()
        var loads = 0
        assertTrue(gate.openOnce(fragment) {
            loads++
            assertFalse(gate.openOnce(fragment) { loads++; true })
            true
        })
        assertEquals(1, loads)
    }

    @Test fun `native navigation exception does not permanently consume activation`() {
        val gate = BrowserHomeRouteGate()
        val fragment = Any()
        runCatching { gate.openOnce(fragment) { throw IllegalStateException("not attached") } }
        assertTrue(gate.openOnce(fragment) { true })
        assertFalse(gate.openOnce(fragment) { true })
    }
    @Test fun `old queued callback cannot consume a later explicit Home request`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        val stale = gate.ticket(home)
        gate.rearm(home)
        val fresh = gate.ticket(home)
        var loads = 0
        assertFalse(gate.openOnce(home, stale) { loads++; true })
        assertTrue(gate.openOnce(home, fresh) { loads++; true })
        assertEquals(1, loads)
    }

    @Test fun `leaving for Mine cancels queued Home but returning explicitly opens again`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        val pending = gate.ticket(home)
        gate.invalidate(home)
        assertFalse(gate.openOnce(home, pending) { true })
        gate.rearm(home)
        assertTrue(gate.openOnce(home) { true })
    }

    @Test fun `Back after a dispatched webpage remains consumed after active leave`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        assertTrue(gate.openOnce(home) { gate.invalidate(home); true })
        assertFalse(gate.openOnce(home) { true })
        gate.rearm(home)
        assertTrue(gate.openOnce(home) { true })
    }

    @Test fun `switching tabs invalidates old work without affecting new tab`() {
        val gate = BrowserHomeRouteGate()
        val first = Any()
        val second = Any()
        val old = gate.ticket(first)
        gate.invalidate(first)
        assertTrue(gate.openOnce(second) { true })
        assertFalse(gate.openOnce(first, old) { true })
        assertFalse(gate.openOnce(second) { true })
    }

    @Test fun `failure in old epoch cannot reset a newer successful request`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        assertFalse(gate.openOnce(home) {
            gate.rearm(home)
            assertTrue(gate.openOnce(home) { true })
            false
        })
        assertFalse(gate.openOnce(home) { true })
    }
    @Test fun `only observed departure and history return reveal native Home`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        assertTrue(gate.openOnce(home) { true })
        gate.returnedHome(home)
        assertFalse(gate.showNativeHome(home))
        gate.leftHome(home)
        gate.returnedHome(home)
        assertTrue(gate.showNativeHome(home))
        assertFalse(gate.openOnce(home) { true })
        gate.rearm(home)
        assertFalse(gate.showNativeHome(home))
        assertTrue(gate.openOnce(home) { true })
    }

    @Test fun `explicit webpage Home before return does not become history fallback`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        assertTrue(gate.openOnce(home) { true })
        gate.leftHome(home)
        gate.rearm(home)
        gate.returnedHome(home)
        assertFalse(gate.showNativeHome(home))
        assertTrue(gate.openOnce(home) { true })
    }
    @Test fun `accepted load without page change recovers native controls without retry loop`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        val token = gate.ticket(home)
        assertTrue(gate.openOnce(home, token) { true })
        assertTrue(gate.needsRecovery(home, token))
        assertTrue(gate.recoverUndispatched(home, token))
        assertTrue(gate.showNativeHome(home))
        assertFalse(gate.openOnce(home) { true })
        assertFalse(gate.recoverUndispatched(home, token))
    }

    @Test fun `late failure recovery cannot reveal native page over a new Home request`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        val old = gate.ticket(home)
        assertTrue(gate.openOnce(home, old) { true })
        gate.rearm(home)
        assertFalse(gate.recoverUndispatched(home, old))
        assertFalse(gate.showNativeHome(home))
        assertTrue(gate.openOnce(home) { true })
        gate.leftHome(home)
        assertFalse(gate.recoverUndispatched(home, gate.ticket(home)))
    }
    @Test fun `repeated explicit Home taps coalesce while dispatch is pending`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        gate.requestHome(home)
        assertTrue(gate.openOnce(home) { true })
        val ticket = gate.ticket(home)
        repeat(3) { gate.requestHome(home) }
        assertEquals(ticket, gate.ticket(home))
        assertFalse(gate.openOnce(home) { true })
        gate.leftHome(home)
        gate.requestHome(home)
        assertTrue(gate.openOnce(home) { true })
    }

    @Test fun `explicit Home can retry once failure fallback restored controls`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        assertTrue(gate.openOnce(home) { true })
        assertTrue(gate.recoverUndispatched(home, gate.ticket(home)))
        gate.requestHome(home)
        assertFalse(gate.showNativeHome(home))
        assertTrue(gate.openOnce(home) { true })
    }
    @Test fun `native invocation failure restores usable Home and requires explicit retry`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        val ticket = gate.ticket(home)
        assertFalse(gate.openOnce(home, ticket) { false })
        assertTrue(gate.failedDispatch(home, ticket))
        assertTrue(gate.showNativeHome(home))
        assertFalse(gate.openOnce(home) { true })
        gate.requestHome(home)
        assertTrue(gate.openOnce(home) { true })
    }

    @Test fun `old failed invocation cannot consume new activation`() {
        val gate = BrowserHomeRouteGate()
        val home = Any()
        val old = gate.ticket(home)
        gate.requestHome(home)
        assertFalse(gate.failedDispatch(home, old))
        assertFalse(gate.showNativeHome(home))
        assertTrue(gate.openOnce(home) { true })
    }
}

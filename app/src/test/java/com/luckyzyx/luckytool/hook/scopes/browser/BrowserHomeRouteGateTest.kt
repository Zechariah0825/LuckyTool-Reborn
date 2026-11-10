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
}

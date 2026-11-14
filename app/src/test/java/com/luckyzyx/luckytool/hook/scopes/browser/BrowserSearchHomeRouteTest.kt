package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserSearchHomeRouteTest {
    @Test fun `native Home activation precedes Search child selection`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any()
        val events = mutableListOf<String>(); var current: Any = Any()
        assertTrue(route.restore(tab, home, {
            assertEquals(BrowserSearchHomeRoute.Phase.RETURNING, route.phase(tab, home))
            events += "returnHome"; current = home
        }, { current === home }, { events += "selectSearch"; true }))
        assertEquals(listOf("returnHome", "selectSearch"), events)
        assertEquals(BrowserSearchHomeRoute.Phase.SEARCH, route.phase(tab, home))
    }
    @Test fun `failed return never selects a different page and stays retryable`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any()
        var selected = false
        assertFalse(route.restore(tab, home, {}, { false }, { selected = true; true }))
        assertFalse(selected)
        assertEquals(BrowserSearchHomeRoute.Phase.FAILED, route.phase(tab, home))
        assertTrue(route.restore(tab, home, {}, { true }, { true }))
    }
    @Test fun `missing Search config or uncommitted selection is not success`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any()
        assertFalse(route.restore(tab, home, {}, { true }, { false }))
        assertEquals(BrowserSearchHomeRoute.Phase.FAILED, route.phase(tab, home))
    }
    @Test fun `tab switch during selection cannot mark success`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any(); var active = true
        assertFalse(route.restore(tab, home, {}, { active }, { active = false; true }))
    }
    @Test fun `explicit Home during native callback cancels old request`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any()
        assertFalse(route.restore(tab, home, { route.cancel(home) }, { true }, { fail("stale selection"); true }))
        assertNull(route.phase(tab, home))
    }
    @Test fun `reentrant lifecycle does not start a second transaction`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any()
        assertTrue(route.restore(tab, home, {
            assertFalse(route.restore(tab, home, { fail("nested return") }, { true }, { true }))
        }, { true }, { true }))
    }
    @Test fun `native exception leaves retry and never clears another tab`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any(); val other = Any()
        route.restore(tab, other, {}, { true }, { true })
        try { route.restore(tab, home, { error("native failure") }, { true }, { true }); fail() }
        catch (_: IllegalStateException) { }
        assertEquals(BrowserSearchHomeRoute.Phase.FAILED, route.phase(tab, home))
        assertEquals(BrowserSearchHomeRoute.Phase.SEARCH, route.phase(tab, other))
        route.cancel(home)
        assertNull(route.phase(tab, home))
        assertEquals(BrowserSearchHomeRoute.Phase.SEARCH, route.phase(tab, other))
    }
    private class Equal { override fun equals(other: Any?) = other is Equal; override fun hashCode() = 1 }
    @Test fun `equal objects cannot impersonate tab or Home`() {
        val route = BrowserSearchHomeRoute(); val tab = Equal(); val home = Equal()
        route.restore(tab, home, {}, { true }, { true })
        assertNull(route.phase(Equal(), home)); assertNull(route.phase(tab, Equal()))
        route.cancel(Equal())
        assertEquals(BrowserSearchHomeRoute.Phase.SEARCH, route.phase(tab, home))
    }
    @Test fun `selection callback cancellation cannot complete an obsolete return`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any()
        assertFalse(route.restore(tab, home, {}, { true }, { route.cancel(home); true }))
        assertNull(route.phase(tab, home))
        assertFalse(route.restore(tab, home, {}, { true }, {
            route.cancel(home)
            assertTrue(route.restore(tab, home, {}, { true }, { true }))
            true
        }))
        assertEquals(BrowserSearchHomeRoute.Phase.SEARCH, route.phase(tab, home))
    }
    @Test fun `transaction temporary Major is retained but later departure relinquishes Search override`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any()
        assertTrue(route.restore(tab, home, {
            assertTrue(route.childChanged(tab, home, false, true))
            assertEquals(BrowserSearchHomeRoute.Phase.RETURNING, route.phase(tab, home))
        }, { true }, { true }))
        assertTrue(route.childChanged(tab, home, true, false))
        assertEquals(BrowserSearchHomeRoute.Phase.SEARCH, route.phase(tab, home))
        assertTrue(route.childChanged(tab, home, false, false))
        assertNull(route.phase(tab, home))
        assertFalse(route.childChanged(tab, home, true, false))
    }
    @Test fun `failed return preserves Major retry but relinquishes state on another child`() {
        val route = BrowserSearchHomeRoute(); val tab = Any(); val home = Any()
        route.restore(tab, home, {}, { true }, { false })
        assertTrue(route.childChanged(tab, home, false, true))
        assertEquals(BrowserSearchHomeRoute.Phase.FAILED, route.phase(tab, home))
        assertTrue(route.childChanged(tab, home, true, false))
        assertNull(route.phase(tab, home))
    }
    @Test fun `root return cancels old permission and cannot enqueue another URL until explicit Home`() {
        val route = BrowserSearchHomeRoute(); val gate = BrowserHomeRouteGate()
        val tab = Any(); val home = Any()
        val old = gate.ticket(home)
        assertTrue(gate.openOnce(home, old) { true })
        gate.leftHome(home)
        gate.invalidate(home)
        gate.failedDispatch(home, gate.ticket(home))
        assertFalse(gate.ownsDispatch(home, old))
        assertTrue(route.restore(tab, home, {
            gate.returnedHome(home)
            assertFalse(gate.openOnce(home, old) { fail("stale URL"); true })
            assertFalse(gate.openOnce(home) { fail("redirect loop"); true })
        }, { true }, { true }))
        route.cancel(home)
        gate.requestHome(home)
        assertTrue(gate.openOnce(home) { true })
    }
    @Test fun `Search to nonexplicit Major consumes transition without authorizing URL`() {
        val route = BrowserSearchHomeRoute(); val gate = BrowserHomeRouteGate()
        val tab = Any(); val home = Any()
        gate.failedDispatch(home, gate.ticket(home))
        route.restore(tab, home, {}, { true }, { true })
        assertTrue(route.childChanged(tab, home, false, true))
        gate.invalidate(home)
        assertNull(route.phase(tab, home))
        assertFalse(gate.openOnce(home) { fail("nonexplicit Major must not reopen URL"); true })
    }
    @Test fun `explicit Home cancels failed Search and authorizes a new URL request`() {
        val route = BrowserSearchHomeRoute(); val gate = BrowserHomeRouteGate()
        val tab = Any(); val home = Any()
        gate.failedDispatch(home, gate.ticket(home))
        route.restore(tab, home, {}, { true }, { false })
        route.cancel(home)
        gate.requestHome(home)
        assertNull(route.phase(tab, home))
        assertTrue(gate.openOnce(home) { true })
    }
}

package com.luckyzyx.luckytool.diagnostics

import org.junit.Assert.*
import org.junit.Test

class HookDiagnosticRequestGateTest {
    private val nonce = "a".repeat(32)
    @Test fun firstValidRequestIsAllowedAtZero() { assertTrue(HookDiagnosticRequestGate().allow(nonce, 0)) }
    @Test fun requestsWithinOneSecondAreRejected() {
        val gate = HookDiagnosticRequestGate()
        assertTrue(gate.allow(nonce, 10)); assertFalse(gate.allow(nonce, 1009)); assertTrue(gate.allow(nonce, 1010))
    }
    @Test fun invalidRequestDoesNotConsumeQuota() {
        val gate = HookDiagnosticRequestGate()
        assertFalse(gate.allow("invalid", 10)); assertTrue(gate.allow(nonce, 10))
    }
    @Test fun backwardOrNegativeTimeNeverBypassesGate() {
        val gate = HookDiagnosticRequestGate()
        assertFalse(gate.allow(nonce, -1)); assertTrue(gate.allow(nonce, 1000)); assertFalse(gate.allow(nonce, 999))
    }
    @Test fun longClockRangeDoesNotOverflow() {
        val gate = HookDiagnosticRequestGate()
        assertTrue(gate.allow(nonce, 0)); assertTrue(gate.allow(nonce, Long.MAX_VALUE))
        assertFalse(gate.allow(nonce, Long.MAX_VALUE))
    }
}

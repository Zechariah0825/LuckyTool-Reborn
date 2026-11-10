package com.luckyzyx.luckytool.diagnostics

/** Cheap validation before building any response. Malformed requests consume no quota. */
class HookDiagnosticRequestGate(private val intervalMs: Long = 1000L) {
    private var last: Long? = null
    @Synchronized fun allow(nonce: String, now: Long): Boolean {
        if (!HookObservationCodec.validNonce(nonce) || now < 0) return false
        val previous = last
        if (previous != null && (now < previous || now - previous < intervalMs)) return false
        last = now
        return true
    }
}

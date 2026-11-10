package com.luckyzyx.luckytool.diagnostics

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HookObservationCodecTest {
    private val nonce = "a".repeat(32)
    private val packageName = "com.example.app"
    private val observation = ProcessObservation(packageName, "$packageName:worker", "b".repeat(32),
        HookObservation.fingerprint("build"), listOf(FeatureObservation("setting_key", HookObservation.config(true),
            registered = 1, triggered = 3, changed = 2, failures = 1, failure = "IllegalStateException", tracksChanges = true)))
    private fun wire() = HookObservationCodec.encode(nonce, observation)
    private fun decode(text: String) = HookObservationCodec.decode(text, nonce, packageName)
    private fun mutated(block: JSONObject.() -> Unit) = JSONObject(wire()).apply(block).toString()
    private fun featureMutation(block: JSONObject.() -> Unit) = mutated { getJSONArray("features").getJSONObject(0).apply(block) }

    @Test fun roundTripPreservesAllEvidenceAndTrackingFlag() { assertEquals(observation, decode(wire())) }
    @Test fun emptyFeatureListRoundTrips() {
        val empty = observation.copy(features = emptyList())
        assertEquals(empty, decode(HookObservationCodec.encode(nonce, empty)))
    }
    @Test fun defaultUntrackedFeatureRoundTrips() {
        val plain = observation.copy(features = listOf(FeatureObservation("plain", HookObservation.config("2"))))
        assertEquals(plain, decode(HookObservationCodec.encode(nonce, plain)))
    }
    @Test fun wrongReplyNonceRejected() { assertNull(decode(mutated { put("nonce", "c".repeat(32)) })) }
    @Test fun invalidRequestedNonceRejected() {
        assertNull(HookObservationCodec.decode(wire(), "bad", packageName))
        assertFalse(HookObservationCodec.validNonce("A".repeat(32)))
        assertFalse(HookObservationCodec.validNonce("a".repeat(31)))
        assertTrue(HookObservationCodec.validNonce(nonce))
    }
    @Test fun wrongPackageRejected() { assertNull(HookObservationCodec.decode(wire(), nonce, "com.other.app")) }
    @Test fun wrongSchemaVersionRejected() { assertNull(decode(mutated { put("v", 2) })) }
    @Test fun missingRequiredFieldRejected() {
        for (field in listOf("v", "nonce", "package", "process", "session", "build", "hostBuild", "features")) {
            assertNull(field, decode(mutated { remove(field) }))
        }
    }
    @Test fun missingRequiredFeatureFieldRejected() {
        for (field in listOf("key", "config", "registered", "triggered", "changed", "failures", "failure", "tracksChanges")) {
            assertNull(field, decode(featureMutation { remove(field) }))
        }
    }
    @Test fun malformedJsonAndWrongContainerRejected() {
        for (bad in listOf("{", "[]", "null", "", "not json")) assertNull(bad, decode(bad))
        assertNull(decode(mutated { put("features", "not an array") }))
        assertNull(decode(mutated { put("features", JSONArray().put("not an object")) }))
    }
    @Test fun oversizedAsciiPayloadRejected() { assertNull(decode(" ".repeat(HookObservationCodec.MAX_BYTES + 1))) }
    @Test fun utf8ByteLimitCheckedSeparatelyFromCharacterCount() {
        val text = mutated { put("extra", "界".repeat(HookObservationCodec.MAX_BYTES / 3)) }
        assertTrue(text.length < HookObservationCodec.MAX_BYTES)
        assertTrue(text.toByteArray(Charsets.UTF_8).size > HookObservationCodec.MAX_BYTES)
        assertNull(decode(text))
    }
    @Test fun encoderRejectsOversizedPayload() {
        val huge = observation.copy(features = listOf(observation.features.single().copy(failure = "x".repeat(HookObservationCodec.MAX_BYTES))))
        try { HookObservationCodec.encode(nonce, huge); fail("must reject oversized payload") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun negativeCountersRejected() {
        for (field in listOf("registered", "triggered", "changed", "failures")) assertNull(field, decode(featureMutation { put(field, -1) }))
    }
    @Test fun countersAboveIntRangeRejectedRatherThanWrapped() {
        for (field in listOf("registered", "triggered", "changed", "failures")) {
            assertNull(field, decode(featureMutation { put(field, Int.MAX_VALUE.toLong() + 1) }))
            assertNull(field, decode(featureMutation { put(field, Long.MAX_VALUE) }))
        }
    }
    @Test fun intBoundaryAccepted() {
        val text = featureMutation {
            put("registered", Int.MAX_VALUE); put("triggered", Int.MAX_VALUE)
            put("changed", Int.MAX_VALUE); put("failures", Int.MAX_VALUE)
        }
        assertEquals(Int.MAX_VALUE, decode(text)!!.features.single().triggered)
    }
    @Test fun duplicateFeatureKeysRejected() {
        assertNull(decode(mutated { val list = getJSONArray("features"); list.put(JSONObject(list.getJSONObject(0).toString())) }))
    }
    @Test fun changedAboveTriggeredRejected() { assertNull(decode(featureMutation { put("changed", 4) })) }
    @Test fun invalidConfigAndBuildHashesRejected() {
        for (bad in listOf("", "a".repeat(63), "A".repeat(64), "g".repeat(64), "private-url")) {
            assertNull(bad, decode(mutated { put("build", bad) }))
            assertNull(bad, decode(featureMutation { put("config", bad) }))
        }
    }
    @Test fun unsafeNamesAndSessionRejected() {
        assertNull(decode(mutated { put("process", "app name\n") }))
        assertNull(decode(mutated { put("session", "bad") }))
        assertNull(decode(featureMutation { put("key", "url/secret") }))
        assertNull(decode(featureMutation { put("failure", "error: private details") }))
        assertNull(decode(featureMutation { put("key", "x".repeat(201)) }))
    }
    @Test fun moreThanMaximumFeaturesRejectedOnDecode() {
        assertNull(decode(mutated {
            put("features", JSONArray().apply {
                repeat(HookObservation.MAX_FEATURES + 1) { put(JSONObject(wire()).getJSONArray("features").getJSONObject(0).put("key", "key_$it")) }
            })
        }))
    }
    @Test fun encoderBoundsFeatureCount() {
        val many = observation.copy(features = (0..HookObservation.MAX_FEATURES).map { observation.features.single().copy(key = "key_$it") })
        assertEquals(HookObservation.MAX_FEATURES, decode(HookObservationCodec.encode(nonce, many))!!.features.size)
    }
    @Test fun schemaVersionRejectsNumericStringsAndFractions() {
        for (bad in listOf<Any>("1", 1.5, true, JSONObject.NULL)) {
            assertNull(bad.toString(), decode(mutated { put("v", bad) }))
        }
    }
    @Test fun countersRejectStringNumbersAndFractions() {
        for (field in listOf("registered", "triggered", "changed", "failures")) {
            for (bad in listOf<Any>("1", 1.5, true, JSONObject.NULL)) {
                assertNull("$field=$bad", decode(featureMutation { put(field, bad) }))
            }
        }
    }
    @Test fun trackingFlagRequiresActualBoolean() {
        for (bad in listOf<Any>("true", "false", 1, 0, JSONObject.NULL)) {
            assertNull(bad.toString(), decode(featureMutation { put("tracksChanges", bad) }))
        }
    }
    @Test fun hostFingerprintRoundTripsWithoutExposingInstallPath() {
        val path = "/data/app/private-install-id/base.apk"
        val host = observation.copy(hostBuild = HookObservation.fingerprint(path))
        val text = HookObservationCodec.encode(nonce, host)
        assertEquals(host, decode(text))
        assertFalse(text.contains(path))
    }
    @Test fun emptyHostFingerprintIsExplicitlyUnknownAndRoundTrips() {
        assertEquals("", JSONObject(wire()).getString("hostBuild"))
        assertEquals("", decode(wire())!!.hostBuild)
    }
    @Test fun malformedHostFingerprintsRejected() {
        for (bad in listOf("a".repeat(63), "a".repeat(65), "A".repeat(64), "g".repeat(64), "/data/app/base.apk", " ")) {
            assertNull(bad, decode(mutated { put("hostBuild", bad) }))
        }
    }
    @Test fun missingOrNullHostFieldIsNotSilentlyAcceptedAsUnknown() {
        assertNull(decode(mutated { remove("hostBuild") }))
        assertNull(decode(mutated { put("hostBuild", JSONObject.NULL) }))
    }
}

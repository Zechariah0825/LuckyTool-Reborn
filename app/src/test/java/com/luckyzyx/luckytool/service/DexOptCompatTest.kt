package com.luckyzyx.luckytool.service

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeoutException

class DexOptCompatTest {
    @Test fun `command uses discrete arguments and accepts framework package`() {
        assertEquals(listOf("/system/bin/cmd", "package", "compile", "-f", "-m",
            "speed-profile", "com.android.systemui"),
            DexOptCompat.command("com.android.systemui", "speed-profile"))
        assertTrue(DexOptCompat.validPackage("android"))
        assertTrue(DexOptCompat.validPackage("com.vendor.app_2"))
    }

    @Test fun `reject option injection shell text malformed or oversized package`() {
        listOf("", "-a", "--reset", "a b", "com.app;id", "com.app\n", "a..b",
            "a/b", "a\u0000b", "a." , "a".repeat(256)).forEach {
            assertFalse(it, DexOptCompat.validPackage(it))
            assertThrows(IllegalArgumentException::class.java) { DexOptCompat.command(it, "speed") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            DexOptCompat.command("com.app", "speed --reset")
        }
    }

    @Test fun `only complete unambiguous success with zero exit is accepted`() {
        assertTrue(DexOptCompat.successful(0, "Success\n"))
        assertTrue(DexOptCompat.successful(0, "Status: compiled\n Success \n\n"))
        listOf("", "Failure", "Success\nFailure", "Failure\nSuccess", "Error: denied\nSuccess",
            "java.lang.Exception: denied\nSuccess", "Success-ish").forEach {
            assertFalse(it, DexOptCompat.successful(0, it))
        }
        assertFalse(DexOptCompat.successful(1, "Success"))
        assertFalse(DexOptCompat.successful(0, "Success", truncated = true))
    }

    @Test fun `Android 17 skips removed legacy entry point`() {
        assertTrue(DexOptCompat.perform(37, { error("Must not call legacy") }, { true }))
        assertTrue(DexOptCompat.perform(38, { error("Must not call legacy") }, { true }))
    }

    @Test fun `older releases preserve legacy result without redundant compile`() {
        assertTrue(DexOptCompat.perform(36, { true }, { error("Unexpected fallback") }))
        assertFalse(DexOptCompat.perform(36, { false }, { error("Unexpected fallback") }))
    }

    @Test fun `backported removed methods use ART and retain failure`() {
        assertTrue(DexOptCompat.perform(36, { throw NoSuchMethodError() }, { true }))
        assertFalse(DexOptCompat.perform(35, { throw NoSuchMethodError() }, { false }))
        assertTrue(DexOptCompat.perform(36, { throw AbstractMethodError() }, { true }))
        assertThrows(SecurityException::class.java) {
            DexOptCompat.perform(36, { throw SecurityException() }, { error("Unexpected retry") })
        }
    }

    @Test fun `process runner drains output with a strict cap`() {
        val result = DexOptCompat.execute(listOf("/bin/sh", "-c",
            "i=0; while [ \"\$i\" -lt 6000 ]; do echo 1234567890; i=\$((i+1)); done; echo Success"), 5000)
        assertEquals(DexOptCompat.OUTPUT_LIMIT, result.output.toByteArray().size)
        assertTrue(result.truncated)
        assertFalse(result.success)
    }

    @Test fun `process runner detects success and nonzero exit`() {
        assertTrue(DexOptCompat.execute(listOf("/bin/sh", "-c", "echo Success"), 5000).success)
        assertFalse(DexOptCompat.execute(listOf("/bin/sh", "-c", "echo Success; exit 1"), 5000).success)
    }

    @Test(timeout = 5000) fun `silent process cannot wait forever before output read`() {
        assertThrows(TimeoutException::class.java) {
            DexOptCompat.execute(listOf("/bin/sh", "-c", "exec sleep 10"), 100)
        }
    }
}

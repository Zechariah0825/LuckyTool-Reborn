package com.luckyzyx.luckytool.service

import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Android-free command policy, also exercised by local JVM tests. */
internal object DexOptCompat {
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*")
    private val filterPattern = Regex("[a-z][a-z0-9-]*")
    private val failurePattern = Regex("(?i)\\b(failure|failed|error|exception)\\b")
    const val TIMEOUT_SECONDS = 180L
    const val OUTPUT_LIMIT = 32 * 1024

    fun validPackage(packageName: String) =
        packageName.length in 1..255 && packagePattern.matches(packageName)

    fun command(packageName: String, compilerFilter: String): List<String> {
        require(validPackage(packageName)) { "Invalid package name" }
        require(compilerFilter.length in 1..64 && filterPattern.matches(compilerFilter)) {
            "Invalid compiler filter"
        }
        return listOf("/system/bin/cmd", "package", "compile", "-f", "-m", compilerFilter, packageName)
    }

    fun successful(exitCode: Int, output: String, truncated: Boolean = false): Boolean =
        !truncated && exitCode == 0 && !failurePattern.containsMatchIn(output) &&
            output.lineSequence().lastOrNull { it.isNotBlank() }?.trim() == "Success"

    fun perform(sdk: Int, legacy: () -> Boolean, art: () -> Boolean): Boolean {
        if (sdk >= 37) return art()
        return try {
            legacy()
        } catch (_: NoSuchMethodError) {
            art()
        } catch (_: AbstractMethodError) {
            art()
        }
    }

    data class Result(val exitCode: Int, val output: String, val truncated: Boolean) {
        val success get() = successful(exitCode, output, truncated)
    }

    /** Drain concurrently: neither a full pipe nor a missing EOF may bypass the deadline. */
    fun execute(command: List<String>, timeoutMillis: Long = TIMEOUT_SECONDS * 1000): Result {
        require(timeoutMillis > 0)
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val reader = Executors.newSingleThreadExecutor { task ->
            Thread(task, "dexopt-output").apply { isDaemon = true }
        }
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        val output = reader.submit<Pair<String, Boolean>> {
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var truncated = false
            process.inputStream.use { stream ->
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    val keep = minOf(count, OUTPUT_LIMIT - bytes.size())
                    bytes.write(buffer, 0, keep)
                    if (keep < count) truncated = true
                }
            }
            bytes.toString(Charsets.UTF_8.name()) to truncated
        }
        try {
            process.outputStream.close()
            if (!process.waitFor(maxOf(0L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) {
                throw TimeoutException("ART compilation timed out")
            }
            val (text, truncated) = output.get(
                maxOf(0L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS
            )
            return Result(process.exitValue(), text, truncated)
        } finally {
            // Never wait indefinitely for a process or a descendant holding its output pipe.
            process.destroyForcibly()
            output.cancel(true)
            reader.shutdownNow()
        }
    }
}

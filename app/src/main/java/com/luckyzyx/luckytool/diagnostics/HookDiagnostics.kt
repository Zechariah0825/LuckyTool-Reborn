package com.luckyzyx.luckytool.diagnostics

import android.os.SystemClock
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.luckyzyx.luckytool.BuildConfig

object HookDiagnostics {
    val store = HookObservationStore()
    private val attachments = HookDiagnosticAttachments()
    @Synchronized fun attach(param: PackageParam) {
        runCatching {
            val packageName = param.packageName; val processName = param.processName
            attachments.attach(packageName, processName) { session ->
                val build = buildFingerprint(param.module.sourceDir)
                val hostBuild = HookObservation.fingerprint(param.appInfo.sourceDir.orEmpty())
                val channel = param.dataChannel
                val requestGate = HookDiagnosticRequestGate()
                channel.wait<String>(HookObservationCodec.REQUEST) { nonce ->
                    runCatching request@{
                        val now = SystemClock.elapsedRealtime()
                        if (!requestGate.allow(nonce, now)) return@request
                        channel.put(HookObservationCodec.RESPONSE, HookObservationCodec.encode(nonce,
                            ProcessObservation(packageName, processName, session, build, store.snapshot(), hostBuild)))
                    }
                }
            }
        }
    }
    /** Normal APK replacement changes this path, including same-version reinstalls. */
    fun buildFingerprint(sourceDir: String): String = HookObservation.fingerprint(
        "${BuildConfig.VERSION_CODE}:${BuildConfig.VERSION_NAME}:$sourceDir")
}

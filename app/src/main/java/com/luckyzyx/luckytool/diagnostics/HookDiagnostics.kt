package com.luckyzyx.luckytool.diagnostics

import android.os.SystemClock
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.luckyzyx.luckytool.BuildConfig
import java.util.UUID

object HookDiagnostics {
    val store = HookObservationStore()
    private var attached = false
    @Synchronized fun attach(param: PackageParam) {
        if (attached) return
        runCatching {
            val packageName = param.packageName; val processName = param.processName
            val session = UUID.randomUUID().toString().replace("-", "")
            val build = buildFingerprint(param.module.sourceDir)
            val channel = param.dataChannel
            var lastRequest = Long.MIN_VALUE
            channel.wait<String>(HookObservationCodec.REQUEST) { nonce ->
                runCatching request@{
                    val now = SystemClock.elapsedRealtime()
                    if (!HookObservationCodec.validNonce(nonce)) return@request
                    if (lastRequest != Long.MIN_VALUE && now - lastRequest < 1000) return@request
                    lastRequest = now
                    channel.put(HookObservationCodec.RESPONSE, HookObservationCodec.encode(nonce,
                        ProcessObservation(packageName, processName, session, build, store.snapshot())))
                }
            }
            attached = true
        }
    }
    /** Normal APK replacement changes this path, including same-version reinstalls. */
    fun buildFingerprint(sourceDir: String): String = HookObservation.fingerprint(
        "${BuildConfig.VERSION_CODE}:${BuildConfig.VERSION_NAME}:$sourceDir")
}

package com.luckyzyx.luckytool.diagnostics

import android.content.Context
import android.os.SystemClock
import com.highcapable.yukihookapi.hook.factory.dataChannel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

object HookDiagnosticClient {
    val observations = MutableStateFlow<Map<String, List<ReceivedObservation>>>(emptyMap())
    private val subscriptions = mutableSetOf<String>()
    private val pending = mutableMapOf<String, Pair<String, Long>>()
    @Synchronized fun request(context: Context, packageName: String) {
        runCatching {
            val channel = context.applicationContext.dataChannel(packageName)
            if (packageName !in subscriptions) {
                channel.wait<String>(HookObservationCodec.RESPONSE) { accept(packageName, it, SystemClock.elapsedRealtime()) }
                subscriptions.add(packageName)
            }
            val now = SystemClock.elapsedRealtime(); val active = pending[packageName]
            if (active != null && now - active.second < 2500) return
            val nonce = UUID.randomUUID().toString().replace("-", "")
            pending[packageName] = nonce to now
            channel.put(HookObservationCodec.REQUEST, nonce)
        }
    }
    @Synchronized private fun accept(packageName: String, encoded: String, now: Long) {
        val (nonce, sentAt) = pending[packageName] ?: return
        if (now < sentAt || now - sentAt > 5000) return
        val observation = HookObservationCodec.decode(encoded, nonce, packageName) ?: return
        val previous = observations.value[packageName].orEmpty().filter {
            it.observation.processName != observation.processName && now - it.receivedAt <= HookObservation.FRESH_MS
        }.takeLast(15)
        observations.value = observations.value + (packageName to (previous + ReceivedObservation(observation, now)))
    }
}

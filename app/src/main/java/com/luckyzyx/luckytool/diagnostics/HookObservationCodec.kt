package com.luckyzyx.luckytool.diagnostics

import org.json.JSONArray
import org.json.JSONObject

/** Strict bounded wire format; no custom objects, raw preferences, URLs or error messages. */
object HookObservationCodec {
    const val REQUEST = "reborn.hook_diagnostics.request.v1"
    const val RESPONSE = "reborn.hook_diagnostics.response.v1"
    const val MAX_BYTES = 64 * 1024
    private val noncePattern = Regex("[a-f0-9]{32}")
    private val namePattern = Regex("[A-Za-z0-9_.:$-]{1,200}")
    private val digestPattern = Regex("[a-f0-9]{64}")
    fun validNonce(nonce: String) = noncePattern.matches(nonce)
    fun encode(nonce: String, observation: ProcessObservation): String = JSONObject().apply {
        put("v", 1); put("nonce", nonce); put("package", observation.packageName)
        put("process", observation.processName); put("session", observation.session); put("build", observation.build)
        put("hostBuild", observation.hostBuild)
        put("features", JSONArray().apply {
            observation.features.take(HookObservation.MAX_FEATURES).forEach { f -> put(JSONObject().apply {
                put("key", f.key); put("config", f.config); put("registered", f.registered)
                put("triggered", f.triggered); put("changed", f.changed); put("failures", f.failures)
                put("failure", f.failure); put("tracksChanges", f.tracksChanges)
            }) }
        })
    }.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) }
    fun decode(text: String, nonce: String, packageName: String): ProcessObservation? = runCatching {
        require(text.length <= MAX_BYTES && text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES && validNonce(nonce))
        val data = JSONObject(text)
        require(data.get("v") == 1 && data.getString("nonce") == nonce && data.getString("package") == packageName)
        val process = data.getString("process"); require(namePattern.matches(process))
        val session = data.getString("session"); require(validNonce(session))
        val build = data.getString("build"); require(digestPattern.matches(build))
        val hostBuild = data.getString("hostBuild"); require(hostBuild.isEmpty() || digestPattern.matches(hostBuild))
        val features = data.getJSONArray("features"); require(features.length() <= HookObservation.MAX_FEATURES)
        val records = (0 until features.length()).map { i ->
            val f = features.getJSONObject(i)
            val key = f.getString("key"); val config = f.getString("config")
            require(namePattern.matches(key) && digestPattern.matches(config))
            fun count(name: String): Int {
                val raw = f.get(name)
                require(raw is Int || raw is Long)
                return (raw as Number).toLong().also { require(it in 0..Int.MAX_VALUE) }.toInt()
            }
            val failure = f.getString("failure"); require(failure.isEmpty() || namePattern.matches(failure))
            val tracksChanges = f.get("tracksChanges")
            require(tracksChanges is Boolean)
            FeatureObservation(key, config, count("registered"), count("triggered"), count("changed"), count("failures"), failure,
                tracksChanges).also { require(it.changed <= it.triggered) }
        }
        require(records.map { it.key }.distinct().size == records.size)
        ProcessObservation(packageName, process, session, build, records, hostBuild)
    }.getOrNull()
}

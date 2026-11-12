package com.luckyzyx.luckytool.utils

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** The wire format deliberately uses stable preference names, never an application ID. */
internal object PrefsBackupCodec {
    val preferenceNames = listOf(ModulePrefs, IntentPrefs, SettingsPrefs, OtherPrefs)

    /** Reads original LuckyTool base64 JSON as well as plain JSON exports. */
    fun decode(text: String): JSONObject {
        val trimmed = text.trim().removePrefix("\uFEFF")
        val json = JSONObject(if (trimmed.startsWith("{")) trimmed else base64Decode(trimmed))
        require(preferenceNames.any { json.optJSONObject(it) != null }) { "No supported preferences" }
        return json
    }

    /** Optional numeric metadata avoids JSON narrowing a Long or a whole-valued Float. */
    fun numericType(value: Any?): String? = when (value) {
        is Long -> "long"
        is Float -> "float"
        else -> null
    }

    fun recordedType(json: JSONObject, prefsName: String, key: String): String? =
        json.optJSONObject(prefsName)?.takeIf { it.optJSONObject("data") != null }
            ?.optJSONObject("types")?.optString(key)?.takeIf { it.isNotBlank() }

    /** Validate before changing the editor; malformed imports must not erase an existing value. */
    fun put(editor: SharedPreferences.Editor, key: String, value: Any, type: String?) {
        when (type) {
            "long" -> {
                require(value is Number && value.toDouble().isFinite() && value.toDouble() == value.toLong().toDouble())
                editor.putLong(key, value.toLong())
            }
            "float" -> {
                require(value is Number && value.toFloat().isFinite())
                editor.putFloat(key, value.toFloat())
            }
            null -> when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> {
                    require(value.toFloat().isFinite())
                    editor.putFloat(key, value.toFloat())
                }
                is Float -> {
                    require(value.isFinite())
                    editor.putFloat(key, value)
                }
                is String -> editor.putString(key, value)
                is JSONArray -> {
                    val values = HashSet<String>()
                    for (index in 0 until value.length()) {
                        val item = value.get(index)
                        require(item is String) { "Preference set must contain strings" }
                        values.add(item)
                    }
                    editor.putStringSet(key, values)
                }
                else -> error("Unsupported preference value")
            }
            else -> error("Unsupported preference type")
        }
    }
}

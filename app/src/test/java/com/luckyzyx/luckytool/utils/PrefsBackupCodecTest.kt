package com.luckyzyx.luckytool.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrefsBackupCodecTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs get() = context.getSharedPreferences("backup-codec-test", Context.MODE_PRIVATE)

    @Before fun reset() { prefs.edit().clear().commit() }

    @Test fun `legacy base64 flat preferences import without a package identity`() {
        val backup = JSONObject().put("osCode", 17).put(ModulePrefs,
            JSONObject().put("enabled", true).put("home", "https://bing.com"))
        val decoded = PrefsBackupCodec.decode(base64Encode(backup.toString()))
        val data = requireNotNull(decoded.prefsData(ModulePrefs))
        for (key in data.keys()) {
            val editor = prefs.edit()
            PrefsBackupCodec.put(editor, key, data.get(key), null)
            assertTrue(editor.commit())
        }
        assertTrue(prefs.getBoolean("enabled", false))
        assertEquals("https://bing.com", prefs.getString("home", ""))
    }

    @Test fun `old wrapped export remains compatible and target preserved`() {
        val original = JSONObject().put(ModulePrefs, JSONObject().put("target", "remote")
            .put("data", JSONObject().put("flag", true)))
        val decoded = PrefsBackupCodec.decode(base64Encode(original.toString()))
        assertEquals(PrefsTarget.REMOTE, decoded.recordedTarget(ModulePrefs))
        assertTrue(decoded.prefsData(ModulePrefs)!!.getBoolean("flag"))
    }

    @Test fun `plain json accepted and unknown metadata cannot become preferences`() {
        val text = """{"sourcePackage":"com.luckyzyx.luckytool","osCode":17,"ModulePrefs":{"a":1},"other":{"data":{}}}"""
        val decoded = PrefsBackupCodec.decode(text)
        assertEquals(1, decoded.prefsData(ModulePrefs)!!.getInt("a"))
        assertEquals(listOf(ModulePrefs, IntentPrefs, SettingsPrefs, OtherPrefs), PrefsBackupCodec.preferenceNames)
        assertFalse(PrefsBackupCodec.preferenceNames.contains("sourcePackage"))
    }

    @Test fun `malformed or unrelated files rejected`() {
        for (text in listOf("", "not base64!", "{}", "[]", "{\"other\":{}}")) {
            assertThrows(Exception::class.java) { PrefsBackupCodec.decode(text) }
        }
    }

    @Test fun `long type survives small json integer representation`() {
        val backup = JSONObject().put(ModulePrefs, JSONObject()
            .put("data", JSONObject().put("number", 3L))
            .put("types", JSONObject().put("number", PrefsBackupCodec.numericType(3L))))
        val decoded = PrefsBackupCodec.decode(base64Encode(backup.toString()))
        val editor = prefs.edit()
        PrefsBackupCodec.put(editor, "number", decoded.prefsData(ModulePrefs)!!.get("number"),
            PrefsBackupCodec.recordedType(decoded, ModulePrefs, "number"))
        assertTrue(editor.commit())
        assertEquals(3L, prefs.getLong("number", -1))
    }

    @Test fun `whole valued float survives round trip`() {
        val backup = JSONObject().put(ModulePrefs, JSONObject().put("data", JSONObject().put("ratio", 2.0f))
            .put("types", JSONObject().put("ratio", PrefsBackupCodec.numericType(2.0f))))
        val decoded = PrefsBackupCodec.decode(base64Encode(backup.toString()))
        val editor = prefs.edit()
        PrefsBackupCodec.put(editor, "ratio", decoded.prefsData(ModulePrefs)!!.get("ratio"),
            PrefsBackupCodec.recordedType(decoded, ModulePrefs, "ratio"))
        assertTrue(editor.commit())
        assertEquals(2.0f, prefs.getFloat("ratio", -1f))
    }

    @Test fun `legacy large numbers and fractional floats restore`() {
        val data = JSONObject("""{"long":9223372036854775807,"float":1.25}""")
        val editor = prefs.edit()
        for (key in data.keys()) PrefsBackupCodec.put(editor, key, data.get(key), null)
        assertTrue(editor.commit())
        assertEquals(Long.MAX_VALUE, prefs.getLong("long", 0))
        assertEquals(1.25f, prefs.getFloat("float", 0f))
    }

    @Test fun `string sets retain strings and reject mixed arrays without erasing existing data`() {
        val editor = prefs.edit()
        PrefsBackupCodec.put(editor, "names", JSONArray().put("one").put("two"), null)
        assertTrue(editor.commit())
        assertEquals(setOf("one", "two"), prefs.getStringSet("names", emptySet()))
        assertThrows(IllegalArgumentException::class.java) {
            PrefsBackupCodec.put(prefs.edit(), "names", JSONArray().put("three").put(4), null)
        }
        assertEquals(setOf("one", "two"), prefs.getStringSet("names", emptySet()))
    }

    @Test fun `unsupported types and nonfinite values rejected before write`() {
        for (pair in listOf< Pair<Any, String?> >(JSONObject() to null, 1 to "unknown", "1" to "long", 1.5 to "long", Double.POSITIVE_INFINITY to "float")) {
            assertThrows(Exception::class.java) { PrefsBackupCodec.put(prefs.edit(), "bad", pair.first, pair.second) }
        }
        assertFalse(prefs.contains("bad"))
    }
}

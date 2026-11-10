package com.luckyzyx.luckytool.hook.scopes.browser

import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.security.MessageDigest

/** Settings shared by LuckyTool's UI and the browser process. No Android dependencies. */
object RebornBrowserConfig {
    const val HOME_MODE = "reborn_browser_home_mode"
    const val HOME_URL = "reborn_browser_home_url"
    const val HOME_SHORTCUTS = "reborn_browser_home_shortcuts"
    const val HIDE_BOTTOM_TABS = "reborn_browser_hide_bottom_tabs"
    const val HIDE_MINE_PROMOS = "reborn_browser_hide_mine_promos"
    const val SEARCH_PROVIDER = "reborn_browser_search_provider"
    const val SEARCH_EXTEND = "reborn_browser_search_extend"
    const val SEARCH_CUSTOM_NAME = "reborn_browser_search_custom_name"
    const val SEARCH_CUSTOM_URL = "reborn_browser_search_custom_url"
    const val SEARCH_CUSTOM_ENGINES = "reborn_browser_search_custom_engines"

    const val ORIGINAL = "original"
    const val BLANK = "blank"
    const val URL = "url"
    const val SHORTCUTS = "shortcuts"
    const val BING = "bing"
    const val GOOGLE = "google"
    const val CUSTOM = "custom"

    data class Shortcut(val title: String, val url: String)
    data class SearchEngine(val id: String, val label: String, val urlTemplate: String)

    /** The observed five items: 首页 / 视频 / 搜索+ / 小说 / 我的. */
    fun <T> filterFiveBottomTabs(items: List<T>, isKnownModel: (T) -> Boolean): List<T>? {
        if (items.size != 5 || items.any { !isKnownModel(it) }) return null
        return listOf(items[0], items[2], items[4])
    }


    fun validHttpUrl(input: String): String? {
        if (input.length > 2048 || input.any { it.isISOControl() }) return null
        val url = input.trim()
        if (url.isEmpty() || url.any { it.isWhitespace() }) return null
        return try {
            val uri = URI(url)
            if (uri.scheme?.lowercase() !in setOf("https", "http") || uri.host.isNullOrBlank() ||
                uri.rawUserInfo != null || uri.host!!.length > 253 ||
                uri.port !in -1..65535
            ) null else url
        } catch (_: URISyntaxException) {
            // java.net.URI reports malformed escapes/brackets with a checked
            // URISyntaxException, not IllegalArgumentException. Invalid saved
            // preferences must never crash either the settings or host process.
            null
        }
    }

    /** One line per item: display title | https://example.com; invalid lines are ignored. */
    fun parseShortcuts(raw: String): List<Shortcut> = raw.lineSequence()
        .mapNotNull { line ->
            val split = line.indexOf('|')
            if (split <= 0) return@mapNotNull null
            val title = line.substring(0, split).trim()
            val url = validHttpUrl(line.substring(split + 1)) ?: return@mapNotNull null
            if (title.isEmpty() || title.length > 40) null else Shortcut(title, url)
        }.distinctBy { it.url }.take(24).toList()

    fun validSearchTemplate(template: String): String? {
        if (template.countPercentS() != 1) return null
        val normalized = template.trim()
        val probe = normalized.replace("%s", "test")
        if (validHttpUrl(probe) == null) return null
        val uri = URI(probe)
        // Only the path/query may contain the query placeholder. Never allow
        // search text to alter the destination host, userinfo, port or fragment.
        val authorityEnd = normalized.indexOf("://") + 3 + uri.rawAuthority.length
        val placeholder = normalized.indexOf("%s")
        val fragmentStart = normalized.indexOf('#')
        if (placeholder < authorityEnd || (fragmentStart >= 0 && placeholder > fragmentStart)) return null
        return normalized
    }

    /** User-facing validation before saving. Runtime parsing still tolerates older bad entries. */
    fun entriesError(raw: String, search: Boolean): String? {
        val lines = raw.lines().mapIndexedNotNull { index, line ->
            line.takeIf { it.isNotBlank() }?.let { index + 1 to it }
        }
        if (lines.size > 24) return "最多可保存 24 条，请删除多余条目"
        for ((number, line) in lines) {
            val divider = line.indexOf('|')
            if (divider <= 0) return "第 ${number} 行格式错误：名称 | https://网址"
            val title = line.substring(0, divider).trim()
            if (title.isEmpty() || title.length > 40) return "第 ${number} 行名称须为 1–40 个字符"
            val url = line.substring(divider + 1).trim()
            if (search && validSearchTemplate(url) == null) {
                return "第 ${number} 行搜索地址无效，请在路径或查询参数中填写一个 %s"
            }
            if (!search && validHttpUrl(url) == null) return "第 ${number} 行网址无效，请填写完整 http/https 网址"
        }
        return null
    }

    private fun String.countPercentS(): Int = Regex("%s").findAll(this).count()

    fun searchEngine(id: String, customName: String, customTemplate: String): SearchEngine? = when (id) {
        BING -> SearchEngine(BING, "Bing", "https://www.bing.com/search?q=%s")
        GOOGLE -> SearchEngine(GOOGLE, "Google", "https://www.google.com/search?q=%s")
        CUSTOM -> validSearchTemplate(customTemplate)?.let {
            SearchEngine(CUSTOM, customName.trim().take(32).ifBlank { "自定义搜索" }, it)
        }
        else -> null
    }

    /** Each editable line: name | https://domain.example/search?q=%s. */
    fun parseCustomSearchEngines(raw: String): List<SearchEngine> = raw.lineSequence()
        .mapNotNull { line ->
            val separator = line.indexOf('|')
            if (separator <= 0) return@mapNotNull null
            val title = line.substring(0, separator).trim()
            val template = validSearchTemplate(line.substring(separator + 1).trim()) ?: return@mapNotNull null
            if (title.isEmpty() || title.length > 40) return@mapNotNull null
            SearchEngine(customEngineId(template), title, template)
        }.distinctBy { it.urlTemplate }.take(24).toList()

    private fun customEngineId(template: String): String = "custom_sha256_" +
        MessageDigest.getInstance("SHA-256").digest(template.toByteArray(Charsets.UTF_8))
            .take(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    fun searchEngineById(id: String, name: String, template: String, entries: String): SearchEngine? {
        searchEngine(id, name, template)?.let { return it }
        val engines = parseCustomSearchEngines(entries)
        return engines.firstOrNull { it.id == id }
            // Preserve selections saved by the previous development version.
            ?: engines.firstOrNull { "custom_" + it.urlTemplate.hashCode().toUInt().toString(16) == id }
    }

    fun searchUrl(engine: SearchEngine, query: String): String =
        engine.urlTemplate.replace("%s", URLEncoder.encode(query, "UTF-8"))
}

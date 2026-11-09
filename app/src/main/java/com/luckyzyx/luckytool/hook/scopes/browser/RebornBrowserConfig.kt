package com.luckyzyx.luckytool.hook.scopes.browser

import java.net.URI
import java.net.URLEncoder

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
                uri.rawUserInfo != null || uri.fragment != null || uri.host!!.length > 253
            ) null else url
        } catch (_: IllegalArgumentException) {
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
        val probe = template.replace("%s", "test")
        return if (validHttpUrl(probe) != null) template.trim() else null
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
            SearchEngine("custom_" + template.hashCode().toUInt().toString(16), title, template)
        }.distinctBy { it.urlTemplate }.take(24).toList()

    fun searchEngineById(id: String, name: String, template: String, entries: String): SearchEngine? =
        searchEngine(id, name, template) ?: parseCustomSearchEngines(entries).firstOrNull { it.id == id }

    fun searchUrl(engine: SearchEngine, query: String): String =
        engine.urlTemplate.replace("%s", URLEncoder.encode(query, "UTF-8"))
}

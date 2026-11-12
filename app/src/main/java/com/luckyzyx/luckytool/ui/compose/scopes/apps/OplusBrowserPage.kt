package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.hook.scopes.browser.RebornBrowserConfig as C
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.showToast

/**
 * 浏览器页：按主页、标签页、搜索、下载和我的页面统一组织功能。
 * 简洁模式入口只在装有 com.heytap.browser 时出现，点击跳浏览器自身的首页设置页。
 */
object OplusBrowserPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_browser",
        prefsName = ModulePrefs,
        packName = "com.heytap.browser",
        scopes = arrayOf("com.heytap.browser"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        category("主页")
        if (c.checkPackName("com.heytap.browser")) {
            custom(title = c.getString(R.string.browser_concise_mode)) {
                PrefRowInset {
                    PrefRow(
                        title = stringResource(R.string.browser_concise_mode),
                        onClick = {
                            try {
                                Intent().apply {
                                    setClassName(
                                        "com.heytap.browser",
                                        "com.heytap.browser.settings.component.BrowserPreferenceActivity"
                                    )
                                    putExtra(
                                        "key.fragment.name",
                                        "com.heytap.browser.settings.homepage.HomepagePreferenceFragment"
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                    c.startActivity(this)
                                }
                            } catch (_: Exception) {
                                c.showToast("Error: Please check your browser version!")
                            }
                        },
                    )
                }
            }
        }
        list(
            key = C.HOME_MODE,
            title = "主页显示模式",
            entries = arrayOf("原始主页（不替换）", "自定义网址", "纯净空白页", "我的快捷链接"),
            entryValues = arrayOf(C.ORIGINAL, C.URL, C.BLANK, C.SHORTCUTS),
            default = C.ORIGINAL,
            notify = true,
        )
        editText(
            key = C.HOME_URL,
            title = "自定义主页网址",
            hint = "https://www.google.com",
            summary = "仅在「自定义网址」模式下使用（http/https）",
            validate = { value ->
                if (value.isBlank() || C.validHttpUrl(value) != null) null
                else "请输入完整的 http/https 网址"
            },
            notify = true,
        )
        editText(
            key = C.HOME_SHORTCUTS,
            title = "编辑主页快捷链接",
            summary = "每行一个：名称 | https://网址",
            hint = "Google | https://www.google.com",
            dialogMessage = "可增加、编辑、删除或调整顺序；每行一条，调整文本行顺序即可排序。最多 24 条。",
            maxLines = 12,
            validate = { C.entriesError(it, search = false) },
            notify = true,
        )
        category("导航与标签页")
        switch(
            key = C.HIDE_BOTTOM_TABS,
            title = "移除底部「视频」和「小说」",
            summary = "保留首页、搜索+、我的；重新启动浏览器生效",
            notify = true,
        )
        switch(
            key = "remove_browser_window_limit_number",
            title = c.getString(R.string.remove_browser_window_limit_number),
        )
        category("搜索页")
        switch(
            key = "reborn_browser_search_history_only",
            title = "搜索页仅保留搜索历史",
            summary = "移除空搜索页的广告、热榜与推荐，保留搜索、历史和清除历史；重启浏览器生效",
            default = true,
            notify = true,
        )
        switch(
            key = "remove_browser_search_bar_app_promotion",
            title = c.getString(R.string.remove_browser_search_bar_app_promotion),
        )
        category("搜索引擎")
        switch(
            key = C.SEARCH_EXTEND,
            title = "向浏览器原生列表添加 Bing、Google 和自定义引擎",
            summary = "目标 40.10.21.9.6 / 40.10.25.2；实验性，更改后重启浏览器",
            default = false,
            notify = true,
        )
        val customEntries = state.getString(C.SEARCH_CUSTOM_ENGINES, "").orEmpty()
        val customEngines = C.parseCustomSearchEngines(customEntries)
        val savedProvider = state.getString(C.SEARCH_PROVIDER, C.ORIGINAL).orEmpty()
        val savedEngine = C.searchEngineById(savedProvider, "", "", customEntries)
        list(
            key = C.SEARCH_BAR_MODE,
            title = "搜索结果顶部引擎切换栏",
            entries = arrayOf("保留浏览器原始切换栏", "隐藏切换栏", "自选显示的搜索引擎"),
            entryValues = arrayOf(C.ORIGINAL, C.SEARCH_BAR_HIDDEN, C.SEARCH_BAR_CUSTOM),
            default = C.ORIGINAL,
            summary = "自选模式始终保留浏览器当前默认引擎；其他引擎可选。隐藏模式隐藏整栏；更改后重启浏览器",
            notify = true,
        )
        (listOf(C.SearchEngine(C.BING, "Bing", ""), C.SearchEngine(C.GOOGLE, "Google", "")) + customEngines).forEach { engine ->
            switch(
                key = C.searchBarEngineKey(engine.id),
                title = "切换栏显示 ${engine.label}",
                summary = "仅自选模式；此项若为浏览器当前默认则仍保留；仅一个引擎时栏自动隐藏",
                default = true,
                notify = true,
            )
        }
        C.nativeSearchBarChoices.forEach { (id, label) ->
            switch(
                key = C.searchBarEngineKey(id),
                title = "切换栏显示 $label（原生）",
                summary = "仅自选模式；若此项是浏览器默认引擎则固定保留；未提供时不添加",
                default = false,
                notify = true,
            )
        }
        list(
            key = C.SEARCH_PROVIDER,
            title = "应用默认搜索引擎（一次性）",
            entries = (listOf("跟随浏览器设置（含新增引擎）", "Bing", "Google") + customEngines.map { it.label }).toTypedArray(),
            entryValues = (listOf(C.ORIGINAL, C.BING, C.GOOGLE) + customEngines.map {
                // Keep an old saved hash ID visibly selected until the user changes it.
                if (savedProvider.startsWith("custom_") && savedEngine?.id == it.id) savedProvider else it.id
            }).toTypedArray(),
            default = C.ORIGINAL,
            summary = "此处显示上次请求，不代表浏览器当前选择。选定并重启后仅同步一次；之后可在 OPPO 浏览器设置中自由更改并保留",
            onChange = {
                // A fresh request survives ORIGINAL → same engine, even if the
                // browser was not opened between those two module changes.
                state.set(C.SEARCH_PROVIDER_REVISION, java.util.UUID.randomUUID().toString())
            },
            notify = true,
        )
        editText(
            key = C.SEARCH_CUSTOM_ENGINES,
            title = "添加、编辑和删除自定义搜索引擎",
            summary = "每行一个：名称 | https://搜索地址?q=%s",
            hint = "DuckDuckGo | https://duckduckgo.com/?q=%s",
            dialogMessage = "每行添加一条，删除整行即可移除；名称和 URL 都能编辑。保存后会出现在上面的默认搜索引擎列表中。模板必须包含一个 %s。",
            maxLines = 14,
            validate = { C.entriesError(it, search = true) },
            onChange = { value ->
                val current = state.getString(C.SEARCH_PROVIDER, C.ORIGINAL).orEmpty()
                if (current.startsWith("custom_") && C.searchEngineById(current, "", "", value) == null) {
                    state.set(C.SEARCH_PROVIDER, C.ORIGINAL)
                    state.set(C.SEARCH_PROVIDER_REVISION, java.util.UUID.randomUUID().toString())
                }
            },
            notify = true,
        )
        category("下载")
        switch(
            key = "remove_ads_from_download_dialog",
            title = c.getString(R.string.remove_ads_from_download_dialog),
        )
        switch(
            key = "remove_ads_at_download_page_bottom",
            title = c.getString(R.string.remove_ads_at_download_page_bottom),
        )
        category("我的页面")
        switch(
            key = C.HIDE_MINE_PROMOS,
            title = "移除「领现金」及「我的小说」区域",
            summary = "仅处理「我的」中的小说卡片和领现金按钮，保留登录、账户及其他工具",
            notify = true,
        )
    }
}

package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.hook.scopes.browser.BrowserUiCleanup
import com.luckyzyx.luckytool.hook.scopes.browser.BrowserHomeReplacement
import com.luckyzyx.luckytool.hook.scopes.browser.BrowserBottomTabs
import com.luckyzyx.luckytool.hook.scopes.browser.BrowserSearchEngines
import com.luckyzyx.luckytool.hook.scopes.browser.RebornBrowserConfig as C
import com.luckyzyx.luckytool.hook.scopes.browser.RemoveAdsAtDownloadPageBottom
import com.luckyzyx.luckytool.hook.scopes.browser.RemoveAdsFromDownloadDialog
import com.luckyzyx.luckytool.hook.scopes.browser.RemoveBrowserSearchBarAppPromotion
import com.luckyzyx.luckytool.hook.scopes.browser.RemoveBrowserWindowLimitNumber
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookBrowser : YukiBaseHooker() {
    override fun onHook() {
        val hideMine = preferences(ModulePrefs).getBoolean(C.HIDE_MINE_PROMOS, false)
        val hideBottom = preferences(ModulePrefs).getBoolean(C.HIDE_BOTTOM_TABS, false)
        if (hideMine || hideBottom) {
            try { loadHooker(BrowserUiCleanup(hideMine, hideBottom)) }
            catch (t: Throwable) { YLog.error("Reborn: BrowserUiCleanup failed", t) }
        }
        if (hideBottom) {
            try { loadHooker(BrowserBottomTabs()) }
            catch (t: Throwable) { YLog.error("Reborn: BrowserBottomTabs failed", t) }
        }
        val mode = preferences(ModulePrefs).getString(C.HOME_MODE, C.ORIGINAL)
        if (mode != C.ORIGINAL) {
            try {
                loadHooker(BrowserHomeReplacement(
                    mode,
                    preferences(ModulePrefs).getString(C.HOME_URL, ""),
                    preferences(ModulePrefs).getString(C.HOME_SHORTCUTS, ""),
                ))
            } catch (t: Throwable) { YLog.error("Reborn: BrowserHomeReplacement failed", t) }
        }
        val searchProvider = preferences(ModulePrefs).getString(C.SEARCH_PROVIDER, C.ORIGINAL)
        val extendSearch = preferences(ModulePrefs).getBoolean(C.SEARCH_EXTEND, false)
        if (extendSearch || searchProvider != C.ORIGINAL) {
            try {
                loadHooker(BrowserSearchEngines(
                    extendSearch,
                    searchProvider,
                    preferences(ModulePrefs).getString(C.SEARCH_CUSTOM_NAME, "自定义搜索"),
                    preferences(ModulePrefs).getString(C.SEARCH_CUSTOM_URL, ""),
                    preferences(ModulePrefs).getString(C.SEARCH_CUSTOM_ENGINES, ""),
                ))
            } catch (t: Throwable) { YLog.error("Reborn: BrowserSearchEngines failed", t) }
        }
        // Native Reborn settings do not need a DEX scan. Avoid parsing the whole
        // APK when every legacy DexKit feature is disabled.
        val legacyEnabled = listOf(
            "remove_ads_from_download_dialog",
            "remove_ads_at_download_page_bottom",
            "remove_browser_window_limit_number",
            "remove_browser_search_bar_app_promotion",
        ).any { preferences(ModulePrefs).getBoolean(it, false) }
        if (!legacyEnabled) return
        try {
            DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
                //移除下载对话框广告
                if (preferences(ModulePrefs).getBoolean("remove_ads_from_download_dialog", false)) {
                    try {
                        loadHooker(RemoveAdsFromDownloadDialog(dexKitBridge))
                    } catch (t: Throwable) {
                        YLog.error("Reborn: RemoveAdsFromDownloadDialog registration failed", t)
                    }
                }
                //移除下载页面底部广告
                if (preferences(ModulePrefs).getBoolean("remove_ads_at_download_page_bottom", false)) {
                    try {
                        loadHooker(RemoveAdsAtDownloadPageBottom(dexKitBridge))
                    } catch (t: Throwable) {
                        YLog.error("Reborn: RemoveAdsAtDownloadPageBottom registration failed", t)
                    }
                }
                //移除浏览器窗口数量限制
                if (preferences(ModulePrefs).getBoolean("remove_browser_window_limit_number", false)) {
                    try {
                        loadHooker(RemoveBrowserWindowLimitNumber(dexKitBridge))
                    } catch (t: Throwable) {
                        YLog.error("Reborn: RemoveBrowserWindowLimitNumber registration failed", t)
                    }
                }
                //移除浏览器搜索框App推广
                if (preferences(ModulePrefs).getBoolean("remove_browser_search_bar_app_promotion", false)) {
                    try {
                        loadHooker(RemoveBrowserSearchBarAppPromotion(dexKitBridge))
                    } catch (t: Throwable) {
                        YLog.error("Reborn: RemoveBrowserSearchBarAppPromotion registration failed", t)
                    }
                }
            }
        } catch (error: Throwable) {
            YLog.error("Reborn: Browser DexKit initialization failed; native hooks remain installed", error)
        }
//        //移除天气页面广告
//        if (preferences(ModulePrefs).getBoolean("remove_ads_from_weather_page", false)) {
//            loadHooker(RemoveAdsFromWeatherPage)
//        }
    }
}
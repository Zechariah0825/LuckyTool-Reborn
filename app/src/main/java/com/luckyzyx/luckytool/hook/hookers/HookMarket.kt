package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.hook.scopes.market.DefaultExpandUpdateList
import com.luckyzyx.luckytool.hook.scopes.market.MarketHomeTabs
import com.luckyzyx.luckytool.hook.scopes.market.MarketHomeTabSelection
import com.luckyzyx.luckytool.hook.scopes.market.RemoveMarketMinePageAppRecommend
import com.luckyzyx.luckytool.hook.scopes.market.RemoveMarketSplashPageAppRecommend
import com.luckyzyx.luckytool.hook.scopes.market.RemoveMarketUpdateDownloadPageAppRecommend
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookMarket : YukiBaseHooker() {
    override fun onHook() {
        if (preferences(ModulePrefs).getBoolean("reborn_market_remove_featured_tab", false)) {
            try { loadHooker(MarketHomeTabs()) }
            catch (t: Throwable) { YLog.error("Reborn: MarketHomeTabs failed", t) }
            try { loadHooker(MarketHomeTabSelection()) }
            catch (t: Throwable) { YLog.error("Reborn: MarketHomeTabSelection failed", t) }
        }
        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            //移除商店启动页应用推荐
            if (preferences(ModulePrefs).getBoolean("remove_market_splash_page_app_recommend", false)) {
                try {
                    loadHooker(RemoveMarketSplashPageAppRecommend(dexKitBridge))
                } catch (t: Throwable) {
                    YLog.error("Reborn: RemoveMarketSplashPageAppRecommend registration failed", t)
                }
            }
            //移除商店更新下载页面应用推荐
            if (preferences(ModulePrefs).getBoolean("remove_market_update_download_page_app_recommend", false)) {
                try {
                    loadHooker(RemoveMarketUpdateDownloadPageAppRecommend(dexKitBridge))
                } catch (t: Throwable) {
                    YLog.error("Reborn: RemoveMarketUpdateDownloadPageAppRecommend registration failed", t)
                }
            }
            //移除软件商店我的页面应用推荐
            if (preferences(ModulePrefs).getBoolean("remove_market_mine_page_app_recommend", false)) {
                try {
                    loadHooker(RemoveMarketMinePageAppRecommend(dexKitBridge))
                } catch (t: Throwable) {
                    YLog.error("Reborn: RemoveMarketMinePageAppRecommend registration failed", t)
                }
            }
            //默认展开更新列表
            if (preferences(ModulePrefs).getBoolean("default_expand_update_list", false)) {
                try {
                    loadHooker(DefaultExpandUpdateList(dexKitBridge))
                } catch (t: Throwable) {
                    YLog.error("Reborn: DefaultExpandUpdateList registration failed", t)
                }
            }
        }
    }
}

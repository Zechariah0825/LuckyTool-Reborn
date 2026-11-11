package com.luckyzyx.luckytool.hook.scopes.market

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class MarketSearchLanding : YukiBaseHooker() {
    override fun onHook() {
        val presenter = "com.heytap.market.search.core.fragment.home.SearchHomeCardAdapterPresenter"
            .toClassOrNull() ?: return
        val request = "com.nearme.platform.loader.paging.d".toClassOrNull() ?: return
        presenter.resolve().firstMethod {
            name = "\u0792"
            parameters(request, List::class)
        }.hook {
            before {
                val cards = args[1] as? List<*> ?: return@before
                val filtered = MarketSearchRules.filterLanding(cards)
                if (filtered != null) args[1] = ArrayList(filtered)
                else YLog.warn("Reborn: Market search landing card schema changed; keeping native batch")
                // Run the original method, including first-page clear/history insertion,
                // even when every server recommendation in this batch was removed.
            }
        }
    }
}

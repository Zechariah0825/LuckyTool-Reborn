package com.luckyzyx.luckytool.service.tiles

import android.service.quicksettings.TileService
import org.lsposed.lsparanoid.Obfuscate
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.closeCollapse

@Obfuscate
class ChargingTestTile : TileService() {
    override fun onClick() {
        closeCollapse()
        IntentUtils(this).jumpBatteryInfo()
    }
}
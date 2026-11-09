package com.luckyzyx.luckytool.listener

import org.lsposed.lsparanoid.Obfuscate
import com.luckyzyx.luckytool.data.AppInfo

@Obfuscate
interface OnSelectAppInfoListener {
    fun resultSelectAppInfos(list: ArrayList<AppInfo>)
}
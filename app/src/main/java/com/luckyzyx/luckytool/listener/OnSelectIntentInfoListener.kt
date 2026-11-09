package com.luckyzyx.luckytool.listener

import com.luckyzyx.luckytool.data.AppIntentInfo
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
interface OnSelectIntentInfoListener {
    fun resultSelectIntentInfos(list: ArrayList<AppIntentInfo>)
}
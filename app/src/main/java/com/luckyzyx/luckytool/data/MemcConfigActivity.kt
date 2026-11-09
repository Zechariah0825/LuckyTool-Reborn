package com.luckyzyx.luckytool.data

import kotlinx.serialization.Serializable
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Serializable
data class MemcConfigActivity(
    var packName: String,
    var activity: String,
    var type: String
)
package com.luckyzyx.luckytool.hook.utils

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("unused")
class IColorDisplayUtils(val classLoader: ClassLoader?) {

    val serviceName = "color_display"
    val clazz = "android.hardware.display.ColorDisplayManager".toClass(classLoader)
    val internal = "android.hardware.display.ColorDisplayManager\$ColorDisplayManagerInternal"
        .toClass(classLoader)

    fun getInstance(): Any? {
        return internal.resolve().firstMethod {
            name = "getInstance"
            emptyParameters()
        }.invoke()
    }
}
package com.luckyzyx.luckytool.hook.utils

import android.os.IBinder
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class SurfaceControlUtils(
    val classLoader: ClassLoader?,
    val clazz: Class<*>
) {
    constructor(classLoader: ClassLoader?) : this(
        classLoader,
        "android.view.SurfaceControl".toClass(classLoader)
    )

    fun isDisplayToken(): Boolean {
        return clazz.resolve().optional(true).firstMethodOrNull {
            name = "getDynamicDisplayInfo"
            parameters(IBinder::class)
        } != null
    }

    fun getDynamicDisplayInfo(displayId: Long?): Any? {
        if (displayId == null) return null
        return clazz.resolve().firstMethod {
            name = "getDynamicDisplayInfo"
            parameters(Long::class)
        }.invoke(displayId)
    }

    fun getDynamicDisplayInfo(displayToken: IBinder?): Any? {
        return clazz.resolve().firstMethod {
            name = "getDynamicDisplayInfo"
            parameters(IBinder::class)
        }.invoke(displayToken)
    }

    fun getPhysicalDisplayToken(physicalDisplayId: Long?): IBinder? {
        if (physicalDisplayId == null) return null
        return clazz.resolve().optional(true).firstMethodOrNull {
            name = "getPhysicalDisplayToken"
            parameters(Long::class)
        }?.invoke<IBinder>(physicalDisplayId)
    }

    fun getInternalDisplayToken(): IBinder? {
        return clazz.resolve().firstMethod {
            name = "getInternalDisplayToken"
            emptyParameters()
        }.invoke<IBinder>()
    }
}
package com.luckyzyx.luckytool.service

import android.content.Context
import android.content.Intent
import android.content.pm.IPackageManager
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.os.ServiceManager
import android.os.SystemProperties
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.IPackageServiceController
import com.luckyzyx.luckytool.service.base.BaseControllerService
import com.luckyzyx.luckytool.utils.LogUtils
import com.topjohnwu.superuser.ipc.RootService
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object PackagesService : BaseControllerService<IPackageServiceController>() {
    override val TAG = "PackageService"
    override var controllerService: Class<*> = classOf<PackageControllerService>()

    private var pm: IPackageManager? = null
    private var binder: IBinder? = null

    private val recipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            LogUtils.w(TAG, "DeathRecipient", "is dead", true)
            binder?.unlinkToDeath(this, 0)
            binder = null
            pm = null
        }
    }

    override fun getController(iBinder: IBinder?): IPackageServiceController? {
        return IPackageServiceController.Stub.asInterface(iBinder)
    }

    override fun get(context: Context?, result: (IPackageServiceController?) -> Unit) {
        if (controller?.asBinder()?.isBinderAlive == false) controller = null
        super.get(context, result)
    }

    @Obfuscate
    class PackageControllerService : RootService() {
        override fun onBind(intent: Intent) = object : IPackageServiceController.Stub() {
            override fun clearApplicationProfileData(packageName: String) {
                PackagesService.clearApplicationProfileData(packageName)
            }

            override fun performDexOptMode(packageName: String): Boolean {
                return PackagesService.performDexOptMode(packageName)
            }
        }
    }

    private fun getPackageManager(): IPackageManager? {
        if (binder == null || pm == null) {
            binder = ServiceManager.getService("package")
            if (binder == null) return null
            try {
                binder!!.linkToDeath(recipient, 0)
            } catch (e: RemoteException) {
                LogUtils.e(TAG, "getPackageManager", e.toString(), true)
            }
            pm = IPackageManager.Stub.asInterface(binder)
        }
        return pm
    }

    fun clearApplicationProfileData(packageName: String) {
        require(DexOptCompat.validPackage(packageName)) { "Invalid package name" }
        try {
            val pm = getPackageManager() ?: throw IllegalStateException("Package manager unavailable")
            pm.clearApplicationProfileData(packageName)
        } catch (e: RemoteException) {
            LogUtils.e(TAG, "clearApplicationProfileData", e.toString(), true)
            throw IllegalStateException("Profile clearing service unavailable")
        } catch (e: RuntimeException) {
            LogUtils.e(TAG, "clearApplicationProfileData", e.toString(), true)
            throw IllegalStateException("Profile clearing failed")
        } catch (e: LinkageError) {
            // Errors cannot safely cross Binder. Keep profile clearing mandatory and report failure.
            LogUtils.e(TAG, "clearApplicationProfileData", e.toString(), true)
            throw IllegalStateException("Profile clearing API unavailable")
        }
    }

    fun performDexOptMode(packageName: String): Boolean {
        if (!DexOptCompat.validPackage(packageName)) return false
        return try {
            DexOptCompat.perform(Build.VERSION.SDK_INT, legacy = {
                val pm = getPackageManager()
                pm?.performDexOptMode(
                    packageName, SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false),
                    SystemProperties.get("pm.dexopt.install", "speed-profile"), true, true, null
                ) == true
            }, art = { compileWithArtService(packageName) })
        } catch (e: RemoteException) {
            LogUtils.e(TAG, "performDexOptMode", e.toString(), true)
            false
        } catch (e: RuntimeException) {
            LogUtils.e(TAG, "performDexOptMode", e.toString(), true)
            false
        } catch (e: LinkageError) {
            LogUtils.e(TAG, "performDexOptMode", e.toString(), true)
            false
        }
    }

    private fun compileWithArtService(packageName: String): Boolean {
        return try {
            val result = DexOptCompat.execute(DexOptCompat.command(
                packageName, SystemProperties.get("pm.dexopt.install", "speed-profile")
            ))
            if (!result.success) {
                LogUtils.e(TAG, "compileWithArtService",
                    "$packageName: exit=${result.exitCode} truncated=${result.truncated} ${result.output}", true)
            }
            result.success
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            LogUtils.e(TAG, "compileWithArtService", "$packageName: $e", true)
            false
        } catch (e: Exception) {
            LogUtils.e(TAG, "compileWithArtService", "$packageName: $e", true)
            false
        }
    }
}

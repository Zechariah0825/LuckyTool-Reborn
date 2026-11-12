# LuckyTool Reborn community package migration

LuckyTool Reborn is an independent enhanced fork of [LuckyTool by luckyzyx / 憶清鳴](https://github.com/luckyzyx/LuckyTool), not an official upstream release. GPL-3.0 and upstream attribution remain in the repository.

## Identity

- Community application ID (v0.10 onward): `io.github.zechariah0825.luckytoolreborn`
- Display name: `LuckyTool Reborn`
- Legacy application ID (v0.9 and earlier / original): `com.luckyzyx.luckytool`
- Implementation namespace and generated Xposed entry retain `com.luckyzyx.luckytool`; this is not the installed application ID.
- Signing certificate SHA-256: `723b385232dba362e1822702a38d86d486e71c5d73bb104ca4e62093d834ac4c` (same private key as v0.8/v0.9, not the upstream key).

Changing application ID creates a separate installation even with the same signature. It does not overwrite the old app or automatically inherit its settings, root grants, Xposed enablement, URI grants, shortcuts, or Android permissions. Do not enable both modules for the same apps simultaneously.

## 从旧版迁移

1. 在仍可使用的旧版中执行设置备份，把文件保存到自己能够找到的位置。先确认文件已保存，不要先卸载旧版。
2. 安装社区版 `LuckyTool-Reborn-v0.10.apk`，它会与旧版并存。
3. 在 Vector 中禁用旧模块，启用新的 LuckyTool Reborn，并核对需要的作用域；按框架要求重启手机/目标应用。
4. 打开新版，从设置的恢复入口选中旧备份文件。旧 base64 JSON / 明文 JSON 均支持，四个稳定配置集合保持一致。
5. 若备份中的配置存于框架 remote prefs，新模块的框架 service 必须已可用；导入失败会显示失败数量，不会把远端配置悄悄写到本地冒充成功。先完成启用/重启，再重试导入。
6. 按需重新授予 KernelSU Root、安装权限等；恢复后重启受影响的目标应用，核对配置及功能。确认迁移成功后再自行决定是否卸载旧版。

无需、也不会自动复制旧应用的私有目录或修改它的安全权限。失效的旧快捷方式可以在新版重建。备份格式不能推断旧文件里已经丢失的数值类型；新导出增加兼容类型信息以保留 Long/Float。

## Update channel and publication

The in-app updater uses `Zechariah0825/LuckyTool-Reborn`, not the original author's repository. Current community release tags must be `v0.N`, `N >= 10`, with versionCode `23301 + N` and the exact asset name `LuckyTool-Reborn-v0.N.apk`. It rejects old-package releases, ambiguous/mismatched assets, and foreign download URLs. Before installation it also checks the downloaded APK application ID and increasing versionCode; Android enforces update-signature matching. A future change of version scheme must update this policy explicitly.

A community submission is not proof of acceptance or a completed listing. Android 17 / ColorOS 17 / KernelSU / Vector runtime behavior still requires device verification.

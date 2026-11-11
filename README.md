# LuckyTool Reborn

LuckyTool 的实验性修改版本，基于 [luckyzyx/LuckyTool](https://github.com/luckyzyx/LuckyTool)，保留 GPL-3.0 许可证和上游版权声明。

本仓库包含完整 Android 工程。构建直接使用当前提交的源码，不在 CI 中下载另一份源码或向仓库自动提交。

## 内容

- `app/`：应用界面、Xposed 模块和测试。
- `hidden-api-stub/`：编译所需的 Android 隐藏 API 声明。
- `gradle/`、`gradlew`、`settings.gradle.kts`：Gradle Wrapper 和构建配置。
- `.github/workflows/build-reborn.yml`：单元测试、Lint、Debug APK 构建和产物归档。
- [BUILD_REBORN.md](BUILD_REBORN.md)：本地构建与 CI 使用说明。
- [REBORN_NOTES.md](REBORN_NOTES.md)：基础版本修改说明与验证边界。
- [VALIDATION_REBORN_8.md](VALIDATION_REBORN_8.md)：最新浏览器返回边界、浏览器与商店搜索净化、验证结果及签名变更。
- [VALIDATION_REBORN_7.md](VALIDATION_REBORN_7.md)：浏览器主页、搜索源图标与领现金按钮的上轮修复。
- [VALIDATION_REBORN_6.md](VALIDATION_REBORN_6.md)：商店真实页面修复、状态检测覆盖及历史验证结果。
- [docs/HOOK_DIAGNOSTICS.md](docs/HOOK_DIAGNOSTICS.md)：状态证据含义、覆盖范围和限制。
- [VALIDATION_REBORN_5.md](VALIDATION_REBORN_5.md)：v0.4 真机反馈修复、v0.5 验证结果与边界。
- [VALIDATION_REBORN_4.md](VALIDATION_REBORN_4.md)：本轮浏览器/商店定制、实际验证结果与剩余限制。
- [README_UPSTREAM.md](README_UPSTREAM.md)：保留的上游说明。
- [REPOSITORY_BOOTSTRAP.md](REPOSITORY_BOOTSTRAP.md)：源码导入前的仓库说明，作为历史记录保留。

## 构建

需要 JDK 21、Android SDK 37.0、Build Tools 36.0.0 和网络连接：

```sh
bash ./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest
# 完整 Lint 单独运行，当前源码存在已记录的错误
bash ./gradlew --no-daemon :app:lintDebug
```

也可以在 [Actions](https://github.com/Zechariah0825/LuckyTool-Reborn/actions) 中运行 **Build LuckyTool Reborn APK**。APK 编译成功后会上传产物，包含提交信息和 SHA-256 校验值。单元测试是必过检查，失败时保留已构建 APK 并使构建任务失败。完整 Lint 在独立的非阻断任务中运行，实际失败会显示警告和完整报告，因此 workflow 绿色不等于 Lint 无错误。

此前 v0.3 基础版已在本地构建，196 项单元测试通过，其历史结果见 [VALIDATION.md](VALIDATION.md)。最新整合版的修复、验证和限制见 [VALIDATION_REBORN_8.md](VALIDATION_REBORN_8.md)，不能把基础版结果当成本轮功能验证。GitHub Actions 按用户要求暂缓，不将配置存在当作运行成功。

## 使用前注意

这是实验性 Debug 构建，仍使用上游包名 `com.luckyzyx.luckytool`。v0.8 使用新的私有签名，不能直接覆盖已交付的 v0.7；请先导出并确认设置备份，再卸载旧模块、安装新模块、恢复设置并重新确认作用域。新密钥已私有备份以供后续更新使用，未放入仓库。Debug 签名不等于官方签名，也不能覆盖官方版本。普通本地或 CI 构建若未明确配置同一密钥，仍不保证互相覆盖安装。

源码中的 Android 17 / ColorOS 17 兼容处理不等于完整真机验证。涉及系统框架的 Hook 需要在目标设备、ROM 和 LSPosed 版本上逐项测试。编译成功也不代表所有功能在真机上正常。

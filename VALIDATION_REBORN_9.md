# Reborn v0.9 验证记录

版本：`1.3.5_reborn.9`，versionCode `23310`。

## 修改

- 删除 Hook 检测页面、每项检测状态、诊断注册/埋点、观测广播/存储及相关测试代码。保留模块实际功能和原设置键。
- 浏览器和软件商店设置按功能统一分组，不再区分 Reborn 与原有优化。
- Google 搜索源显示内置彩色 G 图标，不依赖网络下载。其他来源仍使用既有有界 favicon 加载逻辑。
- 从 Search+ 点击主页时直接加载自定义 URL，不先回退原生主页，从而保留 Search+ 历史。正常网页 Back 保持原生逻辑。

## 验证

- `:app:compileDebugKotlin` 成功。
- `:app:testDebugUnitTest`：324 项，0 失败，0 错误，0 跳过。新环境首次运行因只读 HOME 无法创建 Robolectric 锁文件失败；将测试 HOME/缓存移到可写目录并配置标准 Maven 源后完整重跑通过，没有过滤测试。
- `:app:assembleDebug` 成功。实际 APK 约 23 MB；`apksigner verify` 通过（v3），ZIP CRC 通过。
- 包名 `com.luckyzyx.luckytool`，minSdk 30 / targetSdk 37，arm64-v8a；已检查启用的桌面入口与现代 Xposed `META-INF/xposed/java_init.list`、浏览器/商店作用域。
- `zipalign -c -P 16 4` 通过；两份 native 库 ELF LOAD 对齐均为 16 KiB。
- 完整 `:app:lintDebug` 已跑完：406 个错误、682 个警告、29 个提示。406 个错误全部为未修改 `strings.xml` 的 `MissingTranslation`，与 v0.8 错误基线相同；并非全绿。未屏蔽规则。
- `git diff --check` 通过。APK SHA-256：`4343c8ba0366d937f580f1e8bda96e467ba2ebfec3cdf015e2103ed7c2da439d`。

## 边界

无 OPPO Find X9s Pro / ColorOS 17 真机连接，未验证设备启动或 Hook 实际运行。Search+ 往返以参考 APK 反编译调用链和纯逻辑测试为依据，见 [详细记录](docs/browser-search-home-v9.md)。启动直接进入自定义主页且不存在 Search+ 历史时，不伪造 Search+ 页面，继续保留此前的主页边界处理。

签名恢复自用户授权的私人备份，证书 SHA-256：`723b385232dba362e1822702a38d86d486e71c5d73bb104ca4e62093d834ac4c`。与 v0.8 相同，保持包名时可覆盖；不能覆盖更早的其他签名版本。私钥不在源码/公开产物中。

GitHub Actions 按用户要求暂缓，配置存在不代表本次执行成功。

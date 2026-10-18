# 屏净 · PingJing v0.1.0

一个面向中国安卓用户的 **简洁、直观、跨品牌** 广告按钮自动跳过开源原型。

> ⚠️ **当前为源码测试版，未编译 APK、未进行安卓真机验收。**
> 功能范围以当前代码为准。不是已经完成的全功能广告过滤器。

## 目前已经编写的功能

- 简洁中文四页面：**总览、应用、记录、设置**；主开关和状态提醒
- Android AccessibilityService：观察当前页面的可访问按钮文字，尝试点击明确的跳过/关闭广告按钮
- 通过 Android 桌面应用可见性查询枚举应用，每个 APP 可单独关停
- APP 专属的**精确按钮文字规则**：如某 APP 中显示“关闭”，手动添加后仅该 APP 生效
- 仅本地保存最近 160 条自动点击尝试日志，可以一键清空
- 安全措施：受保护系统/支付应用禁止操作；密码输入框跳过；只处理明确规则、点击冷却、禁止部分敏感词自定义规则
- 无 ROOT、无 Internet 权限、未上传任何内容
- Android 8.0（API 26）及以上；目标 API 35；Java + Android 原生 View，无额外 UI 库

## 尚未实现 / 不要误认为已完成

- **DNS / 本地 VPN 网络广告拦截**：当前不提供；UI 明确显示“后续版本”
- 通过截图/OCR/AI 自动识别特殊广告、通用屏幕上 X 按钮
- 内置大量 APP 专用规则、在线规则订阅、兼容导入 GKD 规则
- 无障碍服务被手机系统终止后的自动保证恢复
- 真机广告识别率、耗电量、安全性及品牌适配的完整测试
- Google Play 上架合规审查、应用签名、版本更新服务

## 开发与构建

**推荐方法：Android Studio**

1. 安装近期版本 Android Studio 与 Android SDK Platform 35。
2. 用 `File > Open` 打开本项目根目录（包含 `settings.gradle` 的目录）。
3. 同步 Gradle 项目，首次需要互联网下载 Android Gradle Plugin 8.13.2 和 Gradle 8.13。
4. 如 Android Studio 报缺少 Gradle wrapper JAR，可将 Gradle distribution 选为本机 Gradle 8.13，或用下方 GitHub Actions 方法构建。本压缩包**未附标准 Gradle wrapper JAR**，不会声称其可直接通过 `gradlew` 构建。
5. 在 `Build > Build Bundle(s) / APK(s) > Build APK(s)` 构建调试安装包。产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

**没有 Android Studio：GitHub Actions（内含配置）**

1. 新建 GitHub 仓库，并上传**本压缩包根目录中的所有文件**（包括 `.github` 隐藏目录）。
2. 在仓库的 `Actions > Build Android debug APK > Run workflow` 启动工作流，或者推送到 `main/master` 分支触发。
3. 构建成功后在该次工作流的 Artifacts 下载 `PingJing-v0.1.0-debug-apk`。
4. 解压工件、在安卓设备上安装 `app-debug.apk`。**需要自行测试并承担安装未经签名发布版的风险。**

> 此 CI 工作流也尚未在本环境运行验证。`debug` APK 是 Android SDK 自动调试签名版本，仅适合测试，不适合直接正式发布。

详细步骤：`docs/安装与使用.md`。

## 开启防护

1. 安装并打开「屏净」。
2. 点击「自动跳广告总开关」并阅读敏感权限声明。
3. 在系统 `无障碍/已安装的服务` 中找到「屏净 · 自动跳广告」，用户主动授权。
4. 返回 APP 查看状态。普通应用默认参与规则匹配；可在 `应用` 页单独禁用。
5. 对无法识别的按钮，在对应 APP 点击 `＋ 添加规则`，手动输入目标按钮实际文字。

注意：Android 13+ 对部分**侧载** APP 的“受限设置”有额外约束。请只授权自己信任的源码构建。不同 ROM 后台管理可能需要额外设置。

## 关键安全及隐私说明

- 本程序**不拥有**绕过其他 APP 强制视频广告或播放激励奖励的能力。
- 无障碍服务有机会访问界面上可访问的按钮文本。只用于本机匹配，未申请 `INTERNET` 权限，日志只存储**应用包名、触发的规则名称、时间**。
- 绝不保证 100% 识别。误触风险无法彻底消除；如出现误操作立即关停总开关或系统无障碍服务。
- 现阶段无法保证 Google Play 审核通过，商店政策对无障碍服务用途有限制。

## 本地规则逻辑验证

在装有 Java 17 的环境中运行 `tools/test-matcher.sh`，可以测试纯 Java 文字匹配器。

## 协议

项目原始代码以 [MIT](LICENSE) 许可证开放。本版**没有**拷贝 GKD 软件源码或第三方订阅规则。

**Windows 便捷脚本：** `生成APK_Windows.bat` 会调用 `生成APK_Windows.ps1`（需 Android SDK 35，联网下载 Gradle 8.13），当前尚未在 Windows 上执行验证。详情见 `docs/安装与使用.md`。

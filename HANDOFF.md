# HANDOFF — recAIteLAB Android 环境与工程现状

> 给新的 AI 会话/未来的自己：本文件记录**已验证可用**的配置与**已验证的坑**。
> 不要在没读这里的情况下重装任何工具链。

## 1. 路径

| 用途 | 路径 |
|---|---|
| 项目 | `F:\Mr.Peng\code\recAIteLAB` |
| 工具链根 | `F:\AndroidToolchain` |
| JDK | `F:\AndroidToolchain\jdk-17`（Temurin 17.0.20.1+1） |
| Android SDK | `F:\AndroidToolchain\android-sdk` |
| Gradle 缓存 | `F:\AndroidToolchain\gradle-home` |
| AVD（不可用，见 §5） | `F:\AndroidToolchain\android-user\avd` |

**工具链必须在项目目录之外。** 之前装在项目内 `.toolchain\`，导致该目录下的可执行文件写系统临时目录被拒（`os error 5`），adb/emulator 全部失败。搬出后恢复正常。

## 2. 已装组件（均已校验哈希）

- JDK 17.0.20.1+1（SHA-256 与 Adoptium 官方一致）
- cmdline-tools 23.0.0（build 16111833）
- platform-tools 37.0.1（adb 1.0.41）
- platforms: android-36 (rev2)、android-37.2 (rev1)
- build-tools: 36.0.0、37.0.0
- emulator 37.3.5（**这台机器上跑不起来，见 §5**）
- Gradle 9.8.2（wrapper 走腾讯镜像 + 官方 SHA-256）

## 3. 用户级环境变量（已持久化）

```
JAVA_HOME         = F:\AndroidToolchain\jdk-17
ANDROID_HOME      = F:\AndroidToolchain\android-sdk
ANDROID_USER_HOME = F:\AndroidToolchain\android-user
ANDROID_AVD_HOME  = F:\AndroidToolchain\android-user\avd
GRADLE_USER_HOME  = F:\AndroidToolchain\gradle-home
```

旧路径 `.toolchain` 已从 PATH 清除。

## 4. 工程配置

| 项 | 值 |
|---|---|
| 包名 / applicationId | `com.recai.lab` |
| 技术栈 | Kotlin + Jetpack Compose |
| AGP | 9.4.1（**Kotlin 支持已内置**，不要再加 `org.jetbrains.kotlin.android` 插件） |
| Compose 编译器插件 | `org.jetbrains.kotlin.plugin.compose` 2.4.21（必需） |
| Gradle | 9.8.2（wrapper） |
| compileSdk | 37 + `compileSdkMinor = 2`（对应 android-37.2；**不写 minor 会去找没装的 android-37.0**） |
| targetSdk / minSdk | 36 / 24 |
| Compose BOM | 2026.09.00 |
| `local.properties` | `sdk.dir=F:/AndroidToolchain/android-sdk`（已 gitignore） |

## 5. 已知坑与对策

1. **模拟器不可用**：CLI 与 Android Studio 版均在启动后终止；`C:\Users\super\.android\` 下连普通文件都建不了。已判定为环境问题，**不要再尝试**，用真机。
2. **adb 起不来**：报 `cannot open ...\Temp\adb.log: Permission denied` + `failed to start daemon`。
   对策：单独窗口跑一次 `F:\AndroidToolchain\android-sdk\platform-tools\adb.exe nodaemon server`。
3. **PowerShell 语法**：必须 `.\gradlew.bat`；`set "X=Y"` 是 cmd 语法，PowerShell 用 `$env:X='Y'`。
4. **Studio 会生成 `gradle/gradle-daemon-jvm.properties`（toolchainVersion=25）**：会把构建拉向 JDK 25。若出现，删掉；并在 Settings → Build Tools → Gradle 把 Gradle JDK 设为 17。
5. **Gradle 临时目录**：若报 `AccessDeniedException: C:\...\Temp\tempdir_*`，把 `TEMP`/`TMP` 指向项目内目录后再跑。
6. **Studio 若在环境变量变更前启动**，会一直用旧值。改过环境变量后必须**完全退出并重启** Studio。

## 6. 日常命令

```powershell
cd F:\Mr.Peng\code\recAIteLAB
.\gradlew.bat assembleDebug      # 出 APK
.\gradlew.bat installDebug       # 直接装到已连接设备
```

产物：`app\build\outputs\apk\debug\app-debug.apk`

## 7. 真机（当前唯一可用设备通道）

- 序列号 `10AF5T05EL001BF`（vivo V2463A / Android 16 / SDK 36 / arm64-v8a）
- 安装验证：`Success`，启动后 `mCurrentFocus = com.recai.lab/.MainActivity`

## 8. 仓库状态

- 分支 `main`，HEAD `4fff805 chore: project skeleton (Kotlin + Compose)`
- 工作树干净

## 9. 未完成 / 待定

- `.setup\` 下有早期下载/解析脚本（已被 gitignore），按需保留或删除
- 包名 `com.recai.lab`、应用名 `recAIteLAB` 均为临时值，正式立项时需确认
- 模拟器问题未解决（不影响开发，真机可用）

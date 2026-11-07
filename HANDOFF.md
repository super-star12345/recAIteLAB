# HANDOFF — recAIteLAB 工程现状与交接（v4.5.1）

> 给新会话 / 未来的 AI：先读 `CLAUDE.md`、`PRD.md`、`android-ai-chat-vibe-coding-plan.md`，再读本文件。
> Prism Glass 主题改造另见 `docs/prism-theme-baseline.md`（仓库内）与
> `F:\Mr.Peng\code\recAIteLAB Workflows\prism-glass-theme-plan.md`（仓库外）。
> 本文件记录**已验证可用**的环境、**当前实现**、**已知坑**和**下一步**；不要重装工具链。
> 交接文档本身可能处于「已修改未提交」状态，以 `git status` 为准。
> 只在用户明确要求交接时更新本文件，不要每个任务后自动改。

## 0. 当前快照（2026-11-07 20:51）

| 项 | 值 |
|---|---|
| 应用版本 | **4.5.1**（versionCode **451**） |
| 包名 / applicationId | `com.recaitelab.app` |
| 分支 / HEAD | `feature/prism-glass` @ `f8d96e0`（与本地 `main` 同一 commit） |
| 远端 | `origin = https://github.com/super-star12345/recAIteLAB.git`（Public）；`origin/main` = `dfdbd54` |
| 工作树 | 未提交：4 项基线问题修复（`Theme.kt` / `Color.kt` / `MarkdownPreview.kt` / `HistoryViewModel.kt` / `KnowledgeScreen.kt` / 新单测）、`HANDOFF.md`、`docs/`；`local.properties` / `keystore.properties` / `*.jks` / `build/` 被忽略 |
| 编译 / 测试 | `.\gradlew.bat assembleDebug` 通过；`testDebugUnitTest` **58/58** 通过（8 个测试类） |
| 真机 | 已连接：V2463A / Android 17（SDK 37）/ 1440×3168 / density 640；已装修复版 debug `4.5.1 / 451`（2026-11-07 21:01，数据保留） |
| 基线截图 | **10 张已完成**，已按修复后界面重拍（完整 1440×3168）→ `docs/baseline/screenshots/` |
| 下一阶段 | Prism Glass **T1：双主题架构 + 静态预览** |

**推送状态**：本地 `main` 领先 `origin/main` **1 个提交**（`f8d96e0` 对话页 Markdown 标题紧凑化，T0 前置小修）。
`origin/main`（`dfdbd54`）已包含 4.5.1 全部功能、README 与 GPLv3 LICENSE。`feature/prism-glass` 尚未推送。
网络可访问 GitHub 后执行 `git push origin main`（或先 push 分支）。

**重要**：
- `local.properties` 里仍有真实 DeepSeek Key（**不要读取、打印或提交**）。
- Provider / 会话 / 消息 / 知识条目均已持久化；Key 只在 Keystore 加密后落 DataStore。
- 真机数据（2026-11-07 实查）：8 个会话、3 条知识条目（id 2/3/4，均带 tags）、
  2 个 Provider（DeepSeek 为当前，另有 KIMI）；覆盖安装后数据均保留。
- 本 HANDOFF 上次快照（2026-10-31）已明显落后，本次（2026-11-07）按实际代码校正：
  新增主题设置（3 套配色 + 动态取色）、两级设置页、Markdown 渲染与代码块复制、统一错误映射、
  上下文裁剪、58 个 JVM 单测、release 签名与 R8、自适应/单色图标、README + GPLv3。

---

## 1. 路径与工具链

| 用途 | 路径 |
|---|---|
| 项目 | `F:\Mr.Peng\code\recAIteLAB` |
| 工具链根 | `F:\AndroidToolchain`（**必须在项目目录之外，不要重装**） |
| JDK | `F:\AndroidToolchain\jdk-17`（Temurin 17.0.20.1+1） |
| Android SDK | `F:\AndroidToolchain\android-sdk` |
| Gradle 缓存 | `F:\AndroidToolchain\gradle-home` |
| AVD（不可用） | `F:\AndroidToolchain\android-user\avd` |

已装组件：

- JDK 17.0.20.1+1；cmdline-tools 23.0.0；platform-tools 37.0.1（adb 1.0.41）
- platforms: android-36 (rev2)、android-37.2 (rev1)
- build-tools: 36.0.0、37.0.0
- Gradle 9.8.2（wrapper，走腾讯镜像）
- emulator 37.3.5（**本机不可用，不要再尝试**）

用户级环境变量（已持久化）：

```
JAVA_HOME         = F:\AndroidToolchain\jdk-17
ANDROID_HOME      = F:\AndroidToolchain\android-sdk
ANDROID_USER_HOME = F:\AndroidToolchain\android-user
ANDROID_AVD_HOME  = F:\AndroidToolchain\android-user\avd
GRADLE_USER_HOME  = F:\AndroidToolchain\gradle-home
```

旧路径 `.toolchain` 已从 PATH 清除。

---

## 2. 工程配置与依赖

| 项 | 值 |
|---|---|
| 技术栈 | Kotlin + Compose（M3）+ MVVM + Room + DataStore + OkHttp + kotlinx.serialization + Navigation Compose |
| AGP | 9.4.1（**Kotlin 支持已内置，禁止再加 `org.jetbrains.kotlin.android`**） |
| Compose 编译器插件 | `org.jetbrains.kotlin.plugin.compose` 2.4.21 |
| kotlinx.serialization 插件 | `org.jetbrains.kotlin.plugin.serialization` 2.4.21 |
| Gradle | 9.8.2（wrapper）、JDK 17 |
| compileSdk | 37 + `compileSdkMinor = 2`（android-37.2；**不写 minor 会找没装的 android-37.0**） |
| targetSdk / minSdk | 36 / 24 |
| Compose BOM | 2026.09.00 |
| Room | 2.8.5（runtime + KSP compiler） |
| KSP | 2.3.13（独立版本号，兼容 Kotlin 2.4.21） |
| DataStore | Preferences 1.3.0 |
| Navigation | Compose 2.10.2（类型安全 route） |
| Markdown | Mikepenz `multiplatform-markdown-renderer-m3` 0.46.0 |
| 测试 | JUnit 4.13.2（testImplementation） |
| 版本 | versionName **4.5.1** / versionCode **451**（4.5.1 → 451） |
| Release 构建 | `keystore.properties` 四项齐全时启用签名（JKS）；`isMinifyEnabled = true` + `isShrinkResources = true` |
| APK 体积 | debug **约 20.9 MB** |

`app/build.gradle.kts` 里的依赖（无版本目录）：

```
androidx.compose:compose-bom:2026.09.00
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.activity:activity-compose:1.13.0
androidx.compose.material:material-icons-extended
androidx.compose.ui:ui-tooling-preview
androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4
androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4
androidx.navigation:navigation-compose:2.10.2
org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0
com.squareup.okhttp3:okhttp:4.12.0
androidx.room:room-runtime:2.8.5
ksp(androidx.room:room-compiler:2.8.5)
androidx.datastore:datastore-preferences:1.3.0
com.mikepenz:multiplatform-markdown-renderer-m3:0.46.0
testImplementation("junit:junit:4.13.2")
debugImplementation androidx.compose.ui:ui-tooling
```

- Room schema 通过 `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` 导出到
  `app/schemas/com.recaitelab.app.data.local.RecAIteLabDatabase/1.json`，已提交。
- debug 的 DeepSeek 配置只从 `local.properties` 注入 `BuildConfig`；release 的
  `DEEPSEEK_API_KEY` 固定为空。`keystore.properties`、`*.jks`、`*.keystore` 均在 `.gitignore`。
- 发布包已开启 R8 与资源裁剪（`proguard-rules.pro` + 默认 optimize 规则）；本次会话**未构建/冒烟 release 包**。
- Markdown 统一走 `ui/common/MarkdownText.kt`：Mikepenz M3 renderer，自定义代码围栏（语言标识 +
  复制按钮 + 横向滚动），并新增 `MarkdownTextStyle`（`ARTICLE` 文章级 / `CHAT` 紧凑标题）。
- **尚未引入**：Hilt、Retrofit。引入任何新依赖/插件前按 `CLAUDE.md` 先说明用途、版本、体积并等确认。

---

## 3. 代码结构与已实现功能

```
com.recaitelab.app
├── RecAIteLabApplication.kt   # 进程入口：AppContainer.initialize(context)
├── MainActivity.kt            # 主题包裹 + 根 Scaffold + NavigationBar + NavHost（4 Tab + 3 二级页）
├── di/AppContainer.kt         # 手动 DI：Room/DataStore/Key/各 Repository/Organizer
├── domain/model/              # 纯 Kotlin + kotlinx.serialization
│   ├── ChatMessage / ChatCompletionRequest / ChatCompletionChunk
│   ├── ModelListResponse / OpenAiJson
│   ├── ProviderConfig.kt      # ProviderConfig + ProviderType(OPENAI_COMPATIBLE)
│   ├── KnowledgeEntry.kt
│   ├── ContextWindow.kt       # 上下文裁剪（纯函数 + 单测）
│   ├── ThemePreferences.kt    # AppearanceMode / AppColorTheme
│   └── UserError.kt           # 面向用户的错误分类（对话/整理共用）
├── data/local/
│   ├── RecAIteLabDatabase.kt  # Room version 1，schema 导出
│   ├── Converters.kt
│   ├── entity/                # Provider/Conversation/Message/KnowledgeEntry
│   ├── dao/                   # 四个 DAO（Flow + suspend）
│   ├── model/                 # ConversationSummary
│   ├── preferences/           # UserPreferencesRepository（含主题偏好）+ ConversationPreferences
│   └── security/              # ApiKeyCipher（Keystore AES-GCM）+ ApiKeyStore
├── data/remote/
│   ├── ChatProvider.kt        # streamChat(..., temperature: Float? = null) + fetchModels()
│   ├── OpenAiCompatibleProvider.kt
│   ├── ChatProviderFactory.kt
│   ├── SseEventReader.kt      # SSE 行/事件读取
│   ├── SseChatStreamParser.kt # 纯 SSE 增量解析（单测）
│   ├── ModelListParser.kt     # GET /models 解析（单测）
│   └── ApiException.kt
├── data/repository/
│   ├── ChatRepository.kt      # Room 上下文 + 流式回复，发送前过 ContextWindow
│   ├── ConversationRepository.kt
│   ├── ProviderRepository.kt
│   ├── KnowledgeRepository.kt
│   ├── KnowledgeOrganizer.kt  # Prompt、streamChat 收集、落库
│   ├── KnowledgePayloadParser.kt # JSON 解析/兜底（单测）
│   ├── ErrorMapper.kt         # 异常 → UserError，统一错误文案
│   └── ChatConfigurationException.kt
├── ui/theme/                  # 3 套配色 × 深浅（青绿/护眼绿/商务蓝）+ M3 默认字阶
├── ui/navigation/AppRoutes.kt # 4 个一级 Tab + 3 个二级路由
├── ui/chat/                   # ChatScreen / ChatViewModel / ChatUiState
├── ui/history/                # HistoryScreen / HistoryViewModel / HistoryUiState
├── ui/knowledge/              # 列表 + 详情 + ViewModel/UiState
├── ui/settings/               # SettingsScreen（首页）/ AppearanceSettingsScreen / ModelSettingsScreen
├── ui/common/                 # MarkdownText（ARTICLE/CHAT + 代码块复制）+ MarkdownPreview + UserErrorText
├── ui/components/             # PlaceholderScreen 等
└── ui/theme/Type.kt           # AppTypography = Typography()
```

### 3.1 对话

- `OpenAiCompatibleProvider.streamChat()`：`POST {baseUrl}/chat/completions`，`stream = true`；
  OkHttp `Call.enqueue` + `callbackFlow`；`SseChatStreamParser` 纯解析 `delta.content`；
  `[DONE]` / `finish_reason` 结束；取消时 `call.cancel()`。
- **关键**：`openAiJson` 必须 `encodeDefaults = true`，否则 `stream = true` 被省略，服务端按非流式返回。
- **temperature**：DTO/接口/仓库全链路可空，默认 null；`@EncodeDefault(NEVER)` + `explicitNulls = false`
  保证 null 字段完全不发送。已验证：null 时请求体无 `temperature`，显式赋值才包含。
  **当前 UI 未暴露温度输入**（设置页只有 Provider/模型/外观），不要硬编码默认值。
- `ContextWindow.truncate()`：system 全保留 + 最近 **20** 条非 system 历史 + 当前消息；
  Room 中完整历史不受影响。**尚未实现 PRD 里的 32k 字符上限**。
- `ChatViewModel`：发送时先追加 user + assistant 占位，再流式更新；`stop()` 取消任务、保留已输出内容、
  不视为错误；`retry()` 替换末尾 assistant 后重新请求（Snackbar「重试」入口，`canRetry` 为 false 时隐藏）；
  `clear()` 仅 ViewModel 方法，UI 无入口。
- UI：顶栏只显示「Provider / 模型」选择器（无设置齿轮）；模型选择 BottomSheet 内含「去设置」；
  助手消息走 `MarkdownText(style = CHAT)`；失败用 Snackbar + `UserError` 文案；输入栏、停止按钮、空态。
- **未做**：token 统计、思考链、多模态、联网搜索；`clear()` 无 UI 入口。

### 3.2 设置与主题

- 设置首页 `SettingsScreen` 只有两个入口（两级导航）：
  - **外观** → `AppearanceSettingsScreen`：外观模式（跟随系统/浅色/深色）、配色方案（默认/护眼绿/商务蓝）、
    动态取色开关（仅 Android 12+，低版本置灰并提示）；
  - **模型选择** → `ModelSettingsScreen`：Provider 卡片列表（当前项 RadioButton、编辑、删除、添加）、
    内联 Provider 表单（名称 / Base URL / API Key / 拉取模型 / 手动增删 / 选择模型）。
    **没有**独立的 `ProviderEditRoute`，编辑在模型选择页内联完成。
- 主题实现：`ui/theme/Color.kt` 提供 6 个 `ColorScheme`（3 配色 × 深浅）；`RecAIteLABTheme`
  只设置 `colorScheme` + `typography`，**未自定义 shapes**；动态取色开启时覆盖所选配色。
- 主题持久化：DataStore `settings` 的 `theme_mode` / `color_theme` / `dynamic_color_enabled` →
  `SettingsViewModel` combine → `MainActivity` 收集后重组 `RecAIteLABTheme`，切换不重启 Activity。
- **Prism Glass（T1 待做）**：需在此基础上新增 `AppDesignSystem { MATERIAL_3, PRISM_GLASS }`、
  4 种 Prism 配色、`ui/prism/` 包与形状/动效/玻璃组件；默认仍 MATERIAL_3，旧界面不变。
  完整基线与约束见 `docs/prism-theme-baseline.md`。

### 3.3 Provider / 持久化 / 历史 / 知识库

- `ProviderRepository`（Room 后端）：保留 `providers` / `activeProvider` / `activeModel` 三个 StateFlow，
  CRUD 为 `suspend` + `Mutex`；`providerById` 同步读快照。
- 数据库 `recaitelab.db` version 1：`providers` / `conversations` / `messages` / `knowledge_entries`。
  Provider id 为 UUID 字符串（TEXT）；会话 providerId 外键 `SET_NULL`；消息级联删除。
- API Key：`ProviderEntity` 不含 Key；`ApiKeyCipher` 用 AndroidKeyStore 别名
  `recaitelab_api_key_v1` 做 AES-256-GCM，密文 `Base64(iv):Base64(cipher)` 存 DataStore
  `secure_keys` 的 `api_key_<providerId>`；删除 Provider 先删库再删密文；`allowBackup=false`。
- DataStore `settings`：`last_provider_id`、`last_model_id`、`last_conversation_id`、
  `default_provider_seeded`、`theme_mode`、`color_theme`、`dynamic_color_enabled`。
- 播种：`RecAIteLabApplication` → `AppContainer.initialize` 后台协程；仅首次、表为空、debug Key 非空时
  写入 DeepSeek；release 不播种。
- **历史记录 Tab**：Room 摘要（updatedAt 倒序 + 最后一条消息）；新建会话、点击加载并切回聊天、
  长按删除、溢出菜单重命名/删除/「整理为知识」；空态。
- **整理为知识**：HistoryViewModel 调用 `KnowledgeOrganizer`；Loading 对话框禁止重复触发；成功
  Snackbar「已加入知识库」+「查看」跳知识库；失败映射为 `UserError`，菜单可重试。
  `KnowledgeOrganizer` 日志只输出 id/conversationId/fallback，不含对话或知识正文。
- **知识库列表**：`KnowledgeRepository.entries` / `search`，300ms 防抖，标题/摘要/正文匹配；卡片显示
  标题/摘要/tags/更新时间；删除确认；空态「去历史记录整理知识吧」。
- **知识详情**：二级路由 `KnowledgeDetailRoute(entryId)`；标题、tags、整理时间、来源模型、Markdown
  （ARTICLE 样式）；`sourceConversationId` 存在时「查看来源会话」跳回聊天；底栏在二级页隐藏。
- **错误处理**：`ErrorMapper` 把 `ApiException`（401/403→Key、429→限流）、`IOException`、配置异常
  收敛为 `UserError`；`UserErrorText` 统一映射到 `strings.xml`；日志经正则脱敏 Bearer/sk-。

### 3.4 导航与 IME

- `MainActivity`：`Scaffold + NavHost`；一级路由 Chat/History/Knowledge/Settings，二级
  KnowledgeDetail / SettingsAppearance / SettingsModels；Tab 切换
  `saveState/restoreState/launchSingleTop/popUpTo(start)`。
- ViewModel 提到 Activity 作用域，Tab 切换不丢状态。
- 底栏在一级 Tab **始终显示，包括 IME 弹起时**；只有二级页隐藏。底栏不随键盘显隐、不 add imePadding、
  不做高度动画。
- Manifest：Activity `android:windowSoftInputMode="adjustResize"`；`allowBackup="false"`。
- 根 `Scaffold` `contentWindowInsets = WindowInsets(0,0,0,0)`；NavHost `padding + consumeWindowInsets`；
  状态栏由各页 TopAppBar / Scaffold 处理，导航栏由常驻 NavigationBar 处理。
- ChatScreen：内层 `Scaffold` 只保留 topBar，`contentWindowInsets = 0`；content 为明确 `Column`：
  消息区 `Box(weight(1f))`（内部 LazyColumn），`ChatInputBar` 作为最后一个固定子项，始终位于底栏上方。
- IME：只在 `ChatInputBar` 使用一次 `Modifier.imePadding()`；根布局、消息列表、NavigationBar 不消费 IME
  insets。设置页 Provider 表单根保留自身一次 `imePadding()`。
- 图标：自适应图标含 `monochrome` 图层（Android 13+ 主题图标）。

### 3.5 调试配置

`local.properties`（root，已 gitignore，**当前已填真实 Key**）：

```properties
DEEPSEEK_API_KEY=sk-...
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1   # 可省略
DEEPSEEK_MODEL=deepseek-chat                    # 可省略
```

- Gradle 读 `local.properties`，写入 **debug 的 BuildConfig**；release 的 `DEEPSEEK_API_KEY` 固定为空。
- `AppContainer` 首次启动用它播种默认 `ProviderConfig`；Key 加密后写 DataStore，明文不进 Room。
- 之后所有 Provider 读写都走 `AppContainer.providerRepository`；业务代码不直接读 `BuildConfig.DEEPSEEK_*`。
- 改 Key/BaseURL 后必须重新构建安装；debug APK 内含 Key，**不要分发**；`local.properties` 不进 Git。
- 2026-10-31 实测 DeepSeek `/models` 返回 `deepseek-flash`、`deepseek-v4-pro`（不含种子 `deepseek-chat`）；
  拉取后若原选中模型不在列表则自动选第一个，保存后写回 ProviderConfig。

---

## 4. 构建 / 安装 / 运行

```powershell
cd F:\Mr.Peng\code\recAIteLAB
.\gradlew.bat assembleDebug        # 产物：app\build\outputs\apk\debug\app-debug.apk（约 20.9MB）
.\gradlew.bat testDebugUnitTest    # 8 个测试类 / 58 个用例
```

单元测试（JVM，`app/src/test`）：

| 测试类 | 覆盖 |
|---|---|
| `SseEventReaderTest` / `SseChatStreamParserTest` | SSE 行读取与增量解析 |
| `ModelListParserTest` | `/models` 响应解析与异常兜底 |
| `ContextWindowTest` | 上下文裁剪规则 |
| `KnowledgePayloadParserTest` | 知识 JSON 解析与原文兜底 |
| `ErrorMapperTest` | 异常 → `UserError` 分类 |
| `ConversationRepositoryTest` | 会话隔离（A/B 交替、删除后新建） |

安装（真机 vivo）：

```powershell
adb shell input keyevent KEYCODE_HOME   # 先回桌面，避免安装确认被前台应用干扰
adb shell am force-stop com.recaitelab.app
adb push app\build\outputs\apk\debug\app-debug.apk /data/local/tmp/recaitelab-debug.apk
adb shell pm install -r -t /data/local/tmp/recaitelab-debug.apk
adb shell am start -W -n com.recaitelab.app/.MainActivity
```

- 真机：`10AF5T05EL001BF`（vivo V2463A / Android 17 / SDK 37 / arm64-v8a / 1440×3168 / density 640）。
- 2026-11-07 20:41 已用 `pm install -r -t` 覆盖安装 **4.5.1 / 451**（基线 commit `f8d96e0`），数据保留；
  设备可正常连接。
- **锁屏/休眠时 `pm install` 会报 `INSTALL_FAILED_ABORTED: User rejected permissions`**；解锁并回桌面后重试。
- `.\gradlew.bat installDebug` 也常被 vivo 安装确认拦截；优先用 `pm install`。
- 2026-11-07 验证（构建层）：`testDebugUnitTest` 58/58；`assembleDebug` 通过；
  另用 `:app:compileDebugKotlin --rerun` 强制重编译通过。
- 2026-11-07 真机已验证：四 Tab 切换、历史列表、知识库列表/详情（Markdown + 代码块 + 复制按钮）、
  两级设置页、外观页、主题浅/深切换、Markdown 对话渲染；10 张基线截图已产出。
- 真机仍缺验证：流式对话发送/停止/重试（本次输入法把英文注入转成中文候选，未发送新消息）、
  知识整理全流程、模型选择 BottomSheet、护眼绿/商务蓝配色、release 包冒烟。
- release 已配置签名（`keystore.properties` + `recaitelab-release.jks`，均忽略）与 R8/资源裁剪，
  但**尚未构建并真机冒烟 release 包**。

---

## 5. 已知坑与对策

1. **模拟器不可用**：CLI 与 Studio 版都启动后终止，`C:\Users\super\.android\` 无法写普通文件；**不要再尝试**，只用真机。
2. **adb 起不来**：报 `cannot open ...\Temp\adb.log: Permission denied` / `failed to start daemon`。
   对策：先跑一次 `F:\AndroidToolchain\android-sdk\platform-tools\adb.exe nodaemon server`（单独窗口）；有时 `adb devices` 会自动重启 daemon。
3. **PowerShell 语法**：必须 `.\gradlew.bat`；`set "X=Y"` 是 cmd 语法，PowerShell 用 `$env:X='Y'`。
4. **Gradle 临时目录**：若报 `AccessDeniedException: C:\...\Temp\tempdir_*`，把 `TEMP`/`TMP` 指向项目内目录后再跑。
5. **Studio 生成 `gradle/gradle-daemon-jvm.properties`（toolchainVersion=25）**：会拉向 JDK 25；出现就删掉，并把 Gradle JDK 设为 17。
6. **代理 / Git TLS**：Watt Toolkit（SteamTools）开启时会 MITM GitHub，Git 报 `SSL certificate problem`；
   关闭后 TLS 正常。**不要盲目 `http.sslVerify=false`**。
7. **GitHub 网络**：若 `git push` 报 `Failed to connect to github.com port 443`，先 `Test-NetConnection github.com -Port 443`；
   本地提交不受影响，网络恢复后补推。
8. **沙箱写 `.ssh` 被拒**：不要生成/配置 SSH 密钥；走 HTTPS + GCM。
9. **PowerShell 的 HTTPS 失败**：本沙箱内 `Invoke-WebRequest` / `curl.exe` 报 TLS 凭证错误，但 Gradle/JDK
   网络正常。查 Maven 版本或下载依赖请交给 Gradle，或写临时 Java 单文件程序，不要据此判定断网。
10. **Room/KSP 首次构建慢**：首次 `assembleDebug` 可能约 5 分钟，之后增量正常。
11. **Markdown 库体积**：`multiplatform-markdown-renderer-m3:0.46.0` 引入 Compose Multiplatform 适配层；
    debug APK 约 20.9MB。已由用户确认允许，仅此一个渲染库。
12. **Gradle File watcher 噪音**：本沙箱构建偶发 `File watcher server ... Couldn't open current thread, error = 5`
    到 stderr，**构建仍成功**，可忽略，不要据此改配置。
13. **HANDOFF / 截图策略**：HANDOFF 只在用户要求交接时更新；临时截图放 `build\tmp\`
    （已 gitignore，`gradlew clean` 会清空）。Prism T0 的正式基线截图建议放
    `docs/baseline/screenshots/`，清单见 `docs/prism-theme-baseline.md` 第 6 节。
14. **不要每个任务自动装真机/截图/卸载应用**；只有用户明确要求时才操作真机。
15. **`adb shell input text` 会被中文输入法改造**：2026-11-07 实测，注入英文时输入法把部分字母组合成中文候选，
    且 `%s` 空格失效，结果不可用。真机自动化输入中文/英文前，先切换为纯英文输入法或改用剪贴板/其他注入方式；
    不要用 `input text` 的结果判断 UI 是否正常。

---

## 6. Git 状态

- 当前分支 `feature/prism-glass`（T0 新建）@ `f8d96e0`；本地 `main` 同一 commit，领先 `origin/main` 1 个提交。
- `origin/main` = `dfdbd54 docs: add README and GPLv3 license`（已含 4.5.1 全部功能）。
- 已提交：全部源码、`CLAUDE.md`、`PRD.md`、`README.md`、`LICENSE`（GPLv3）、Room schema、JUnit 测试、图标资源。
- 未提交：4 项基线问题修复 + `HANDOFF.md` + `docs/`，等待用户验收后提交：
  - 修改：`ui/theme/Theme.kt`、`ui/theme/Color.kt`、`ui/history/HistoryViewModel.kt`、`ui/knowledge/KnowledgeScreen.kt`
  - 新增：`ui/common/MarkdownPreview.kt`、`app/src/test/.../MarkdownPreviewTest.kt`
  - 文档：`docs/prism-theme-baseline.md`、`docs/baseline/screenshots/`（10 张 PNG，已重拍为修复后状态）
- 忽略：`local.properties`、`keystore.properties`、`*.jks` / `*.keystore`、`build/`、`.gradle/`、`*.apk`、`.idea/`、`.kotlin/`。
- 提交前扫描源码无 `sk-*` 明文；**不得提交** `local.properties`、debug APK、签名材料。
- 网络恢复后：`git push origin main`；如需先备份分支：`git push -u origin feature/prism-glass`。

---

## 7. 已知缺口 / 下一阶段

已完成（截至 2026-11-07）：

- Room 数据层（Provider / Conversation / Message / KnowledgeEntry）+ schema v1；
- Provider 配置、激活项、模型选择持久化；Keystore 加密 Key；DataStore 偏好（含主题）；
- 会话/消息持久化、启动恢复、严格会话隔离（含 JUnit 回归测试）；
- 四 Tab 导航、历史记录（新建/打开/重命名/删除/整理为知识）、知识库列表/搜索/删除/详情/Markdown；
- 知识整理服务（JSON 解析 + 兜底落库）；统一错误映射与重试；
- 对话页 Markdown 渲染（CHAT 紧凑样式）、代码块复制、上下文裁剪（20 条）；
- 主题：外观模式 + 3 套配色 + 动态取色；两级设置导航；自适应/单色图标；
- release 签名与 R8 配置；58 个 JVM 单测；
- **T0 基线问题修复（2026-11-07）**：系统栏图标明暗跟随主题、补全 `surfaceContainer*` 色板、
  历史预览剥离 Markdown（新增 `MarkdownPreview` + 单测）、知识库 tags 换行；已装真机并由用户人工验证通过，
  基线截图已按修复后界面重拍。
- **Prism T0**：`feature/prism-glass` 分支、基线 commit、构建/测试记录、`docs/prism-theme-baseline.md`、
  `docs/baseline/screenshots/` 10 张真机基线截图；真机已装 4.5.1/451。

仍缺 / 下一步：

1. **Prism T1（下一步）**：DataStore 新增 `design_system` / `prism_color_style` / `prism_seed_color` /
   `glass_quality`；建立 `ui/prism/`（PrismTheme、Colors、Shapes、Motion、Components、Navigation、glass）；
   4 种配色 × 深浅；设置页「界面风格」+ 静态预览页；默认仍 MATERIAL_3，不改正式页面。
   设计文档：`F:\Mr.Peng\code\recAIteLAB Workflows\prism-glass-theme-plan.md`（T1–T6）。
2. **T0 基线问题**（详见 `docs/prism-theme-baseline.md` 第 7 节）：
   - 已修复（构建通过，用户人工验证通过）：浅色状态栏图标不可见、`NavigationBar`/知识卡片/FAB 紫调、
     历史预览 Markdown 源码、知识库 tags 被时间覆盖；
   - 保留：聊天 LaTeX 纯文本展示、FAB 遮挡列表末条。
3. 阶段 5 剩余：token 统计、思考链、多模态、联网搜索；`clear()` UI 入口；聊天上下文 32k 字符上限。
4. 阶段 6：release 包真机冒烟、隐私与权限复核；ProviderRepository / 网络层补测试。
5. 真机仍缺验证：流式对话发送/停止/重试、知识整理全流程、模型选择 BottomSheet、
   护眼绿/商务蓝配色、单色图标、release 包。

---

## 8. 约定提醒

- 严格遵循 `CLAUDE.md`：不改既定 SDK/AGP/Gradle/Compose BOM 版本；不加清单外依赖；不用 XML 布局；
  Key 不落日志/不进 Git；用户可见文案进 `strings.xml`；改完必须 `assembleDebug` 通过。
- 状态提升：Screen 只接收 state + 回调，不访问 Repository/ViewModel。
- 网络只走 OkHttp（未引入 Retrofit）；JSON 统一 kotlinx.serialization + `openAiJson`。
- 调试配置只从 `local.properties` 进 BuildConfig（debug）；业务代码不直接读 `BuildConfig.DEEPSEEK_*`。
- 当前版本 **4.5.1 / 451**；真机已装 4.5.1（2026-11-07 覆盖安装）。
- 临时截图 / 调试产物统一放 `build\tmp\`；Prism 基线截图清单见 `docs/prism-theme-baseline.md`。
- 用户要求：**不要每个任务自动安装/打开/截图**；只有明确要求时才装真机。HANDOFF 也只在要求交接时更新。
- Prism Glass 约束（详见 `docs/prism-theme-baseline.md` 第 5 节）：不引入 Flutter、不删除 Material 3、
  不修改 ViewModel/Repository、不复制两套业务逻辑；默认 MATERIAL_3；IME 与底栏常驻行为必须保留。

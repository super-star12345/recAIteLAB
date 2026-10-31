# HANDOFF — recAIteLAB 工程现状与交接（v4.5.1）

> 给新会话 / 未来的 AI：先读 `CLAUDE.md`、`PRD.md`、`android-ai-chat-vibe-coding-plan.md`，再读本文件。
> 本文件记录**已验证可用**的环境、**当前实现**、**已知坑**和**下一步**；不要重装工具链。
> 交接文档本身可能处于「已修改未提交」状态，以 `git status` 为准。
> 只在用户明确要求交接时更新本文件，不要每个任务后自动改。

## 0. 当前快照（2026-10-31 22:20）

| 项 | 值 |
|---|---|
| 应用版本 | **4.5.1**（versionCode **451**） |
| 包名 / applicationId | `com.recaitelab.app` |
| 分支 / HEAD | `main` @ 本地最新提交（上一提交 `cc6721c`；4.5.1 提交未推送） |
| 远端 | `origin = https://github.com/super-star12345/recAIteLAB.git`（Public） |
| 工作树 | 代码与 HANDOFF 已随 4.5.1 本地提交；`local.properties` / `build/` / APK 被忽略 |
| 编译 / 测试 | `.\gradlew.bat assembleDebug` 通过；`testDebugUnitTest` 3/3 通过 |
| 真机 | 已安装 **3.5.2**（2026-10-31 22:20:59）；**4.5.1 尚未安装** |
| 下一阶段 | 阶段 5 体验打磨 / 阶段 6 测试发布 |

**推送状态**：`cc6721c` **尚未推送**。`git push origin main` 报 `Failed to connect to github.com port 443`；
本机 `Test-NetConnection github.com -Port 443` 返回 False，Git 无代理配置。网络恢复后执行
`git push origin main`，当前 `main...origin/main [ahead 1]`。

**重要**：`local.properties` 里仍有真实 DeepSeek Key（不要读取、打印或提交）。Provider / 会话 / 消息 /
知识条目均已持久化；Key 只在 Keystore 加密后落 DataStore。测试期间已真实写入一条知识条目
（id=1, conversationId=2, fallback=false）。

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

`app/build.gradle.kts` 里的依赖（无版本目录）：

```
androidx.compose:compose-bom:2026.09.00
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.activity:activity-compose:1.13.0
androidx.compose.material:material-icons-extended        # debug APK ~24.3MB
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

Room schema 通过 `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` 导出到
`app/schemas/com.recaitelab.app.data.local.RecAIteLabDatabase/1.json`，已提交。

Markdown 渲染统一走 `ui/common/MarkdownText.kt`，使用 Mikepenz M3 renderer；默认 M3
颜色/排版，库默认 typography 中代码块使用 `FontFamily.Monospace`（代码审查确认）。后续对话页直接复用它，
不要重复引库。

**尚未引入**：Hilt、Retrofit。引入任何新依赖/插件前按 `CLAUDE.md` 先说明用途、版本、体积并等确认。

---

## 3. 代码结构与已实现功能

```
com.recaitelab.app
├── RecAIteLabApplication.kt   # 进程入口：AppContainer.initialize(context)
├── MainActivity.kt            # Scaffold + NavigationBar + NavHost，四个一级 Tab + 知识详情二级页
├── di/AppContainer.kt         # 手动 DI：Room/DataStore/Key/各 Repository/Organizer
├── domain/model/              # 纯 Kotlin + kotlinx.serialization
│   ├── ChatMessage / ChatCompletionRequest / ChatCompletionChunk
│   ├── ModelListResponse      # GET /models 的 data[].id
│   ├── OpenAiJson.kt          # 共享 Json 配置
│   ├── ProviderConfig.kt      # ProviderConfig + ProviderType(OPENAI_COMPATIBLE)
│   └── KnowledgeEntry.kt      # 知识条目领域模型
├── data/local/
│   ├── RecAIteLabDatabase.kt  # Room 数据库单例（version 1，schema 导出）
│   ├── Converters.kt          # List<String> ↔ JSON、ProviderType ↔ String
│   ├── entity/                # Provider/Conversation/Message/KnowledgeEntry 实体 + domain 映射
│   ├── dao/                   # 四个 DAO（Flow + suspend）
│   ├── model/                 # ConversationSummary（会话列表摘要 POJO）
│   ├── preferences/           # UserPreferencesRepository + ConversationPreferences 接口
│   └── security/              # ApiKeyCipher（Keystore AES-GCM）+ ApiKeyStore（secure_keys）
├── data/remote/
│   ├── ChatProvider.kt        # streamChat(..., temperature: Float? = null) + fetchModels()
│   ├── OpenAiCompatibleProvider.kt  # OkHttp SSE + GET /models
│   ├── ChatProviderFactory.kt # createChatProvider(config) 按 type 分派
│   ├── SseEventReader.kt      # 纯 SSE 行/事件读取
│   └── ApiException.kt        # 非 2xx
├── data/repository/
│   ├── ChatRepository.kt      # Room 上下文 + 流式回复（完成后写 assistant）
│   ├── ConversationRepository.kt  # 会话创建/恢复/隔离、列表摘要、重命名、删除
│   ├── ProviderRepository.kt  # Room + 密文 Key + DataStore
│   ├── KnowledgeRepository.kt # 知识条目 CRUD / Flow / 搜索
│   ├── KnowledgeOrganizer.kt  # Prompt、streamChat 收集、JSON 解析/兜底、落库
│   └── ChatConfigurationException.kt
├── ui/theme/                  # 青绿品牌色、深浅两套 ColorScheme、默认 Typography
├── ui/navigation/AppRoutes.kt # @Serializable 一级 Tab + KnowledgeDetailRoute(entryId)
├── ui/chat/                   # ChatScreen / ChatViewModel / ChatUiState
├── ui/history/                # HistoryScreen / HistoryViewModel / HistoryUiState
├── ui/knowledge/              # 列表 + 详情 + ViewModel/UiState
├── ui/common/MarkdownText.kt  # 可复用 Markdown 组件
├── ui/components/             # PlaceholderScreen 等
└── ui/settings/               # SettingsScreen / SettingsViewModel / SettingsUiState
```

### 3.1 对话

- `OpenAiCompatibleProvider.streamChat()`：`POST {baseUrl}/chat/completions`，`stream = true`，OkHttp
  原生 `Call.enqueue` + `callbackFlow`；逐行读 `data:`，解析 `delta.content`；`[DONE]`/`finish_reason`
  结束；取消时 `call.cancel()`。
- **关键**：`openAiJson` 必须 `encodeDefaults = true`，否则 `stream = true` 被省略，服务端按非流式返回。
- **temperature**：DTO/接口/仓库全链路可空，默认 null；`@EncodeDefault(NEVER)` + `explicitNulls = false`
  保证 null 字段完全不发送，由厂商使用默认值。已用 JVM 序列化验证：null 时请求体无 `temperature`，
  显式赋值时才包含。**不要重新加硬编码 0.7/1.0 等默认值。**
- `ChatRepository`：上下文来自 Room；每次 send/regenerate 时解析激活 Provider 与 `selectedModel`，
  用 `createChatProvider` 按需创建；没有全局内存消息列表。
- `ConversationRepository`：首次发送创建 `ConversationEntity`（标题取首条消息前 20 个码点）；
  发送前写 user 消息，流式完成后整体写 assistant；`last_conversation_id` 存 DataStore，启动恢复。
- **会话隔离修复**：发送路径只在 active 或 `last_conversation_id` 有效时复用会话，**绝不回退到
  `getLatest()`**；新建/删除后首条消息创建新会话。`ConversationRepositoryTest` 覆盖 A/B 交替和删除场景。
- `ChatViewModel`：启动先加载上次会话（`isLoadingHistory` 期间禁用输入）；send/stop/retry/clear；
  `stop()` 保留 UI 内容但不落库半截 assistant；`openConversation` 整体替换消息；`startNewConversation`
  清空上下文；`onConversationDeleted` 重置当前会话。
- UI：顶栏只显示 `Provider / 模型` 选择器（无设置齿轮）；模型选择 BottomSheet 内仍有「去设置」；
  输入栏、消息气泡、停止按钮、空状态、错误 Snackbar。
- **未做**：对话页 Markdown 渲染（`MarkdownText` 已就绪）、代码块复制、token 统计、思考链。
- `retry()` / `clear()` 仍只有 ViewModel 方法，UI 没有入口。

### 3.2 Provider / 持久化 / 历史 / 知识库

- `ProviderRepository`（Room 后端）：保留 `providers` / `activeProvider` / `activeModel` 三个 StateFlow，
  CRUD 为 `suspend` + `Mutex`；`providerById` 同步读快照。
- 数据库 `recaitelab.db` version 1：`providers` / `conversations` / `messages` / `knowledge_entries`。
  Provider id 为 UUID 字符串（TEXT）；会话 providerId 外键 `SET_NULL`；消息级联删除。
- API Key：`ProviderEntity` 不含 Key；`ApiKeyCipher` 用 AndroidKeyStore 别名
  `recaitelab_api_key_v1` 做 AES-256-GCM，密文 `Base64(iv):Base64(cipher)` 存 DataStore
  `secure_keys` 的 `api_key_<providerId>`；删除 Provider 先删库再删密文；`allowBackup=false`。
- DataStore `settings`：`last_provider_id`、`last_model_id`、`last_conversation_id`、
  `default_provider_seeded`。
- 播种：`RecAIteLabApplication` → `AppContainer.initialize` 后台协程；仅首次、表为空、debug Key 非空时
  写入 DeepSeek；release 不播种。
- `SettingsScreen`：Provider 列表/编辑/删除/设为当前；拉取模型、手动增删、选中模型；Key 掩码。
- **历史记录 Tab**：Room 摘要（updatedAt 倒序 + 最后一条消息）；新建会话、点击加载并切回聊天、
  长按删除、溢出菜单重命名/删除/「整理为知识」；空态。
- **整理为知识**：HistoryViewModel 调用 `KnowledgeOrganizer`；Loading 对话框禁止重复触发；成功
  Snackbar「已加入知识库」+「查看」跳知识库；失败映射网络/Key/限流等错误，菜单可重试。
  `KnowledgeOrganizer` 日志只输出 id/conversationId/fallback，不含对话或知识正文。
- **知识库列表**：`KnowledgeRepository.entries` / `search`，300ms 防抖，标题/摘要/正文匹配；卡片显示
  标题/摘要/tags/更新时间；删除确认；空态「去历史记录整理知识吧」。
- **知识详情**：二级路由 `KnowledgeDetailRoute(entryId)`；标题、tags、整理时间、来源模型、Markdown
  正文；`sourceConversationId` 存在时「查看来源会话」跳回聊天；底栏在二级页隐藏。
- **知识整理服务**：读取会话全部消息，拼 `role: content`；20,000 字符首尾截断；固定 Prompt；复用
  `streamChat` 且不传 temperature；首个 `{` 到末个 `}` 解析，兼容 ```json；失败兜底
  「未命名整理」/原文前 100 字/原始输出/空 tags，照常落库。

### 3.3 导航与 IME

- `MainActivity`：`Scaffold + NavHost`；一级路由 Chat/History/Knowledge/Settings，二级
  KnowledgeDetail；Tab 切换 `saveState/restoreState/launchSingleTop/popUpTo(start)`。
- 底栏在一级 Tab 显示；进入知识详情或 IME 弹起时隐藏。ViewModel 提升到 Activity 作用域，Tab 切换不丢状态。
- Manifest：Activity `android:windowSoftInputMode="adjustResize"`。
- 根 `Scaffold` `contentWindowInsets = WindowInsets(0,0,0,0)`；NavHost `padding + consumeWindowInsets`；
  状态栏由各页 TopAppBar / Scaffold 处理，导航栏由 NavigationBar 处理。
- Chat：只在 `ChatInputBar` 使用一次 `Modifier.imePadding()`；根/列表不再叠加；设置页根保留自身一次
  `imePadding()`。无键盘高度监听、无 adjustPan。
- 旧问题已修：Chat 内层 Scaffold 的 imePadding 叠加底栏占位导致输入框上移/回弹与键盘上方空白。

### 3.4 调试配置

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
.\gradlew.bat assembleDebug        # 产物：app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat testDebugUnitTest    # ConversationRepositoryTest，3/3
```

安装（真机 vivo）：

```powershell
adb shell input keyevent KEYCODE_HOME   # 先回桌面，避免安装确认被前台应用干扰
adb shell am force-stop com.recaitelab.app
adb push app\build\outputs\apk\debug\app-debug.apk /data/local/tmp/recaitelab-debug.apk
adb shell pm install -r -t /data/local/tmp/recaitelab-debug.apk
adb shell am start -W -n com.recaitelab.app/.MainActivity
```

- 真机：`10AF5T05EL001BF`（vivo V2463A / Android 16 / SDK 36 / arm64-v8a）。
- **锁屏/休眠时 `pm install` 会报 `INSTALL_FAILED_ABORTED: User rejected permissions`**；解锁并回桌面后重试。
- `.\gradlew.bat installDebug` 也常被 vivo 安装确认拦截；优先用 `pm install`。
- 当前真机已安装 **3.5.2 / 352**（2026-10-31 22:20:59）；**4.5.1 / 451 尚未安装**。`pm install -r` 保留数据；卸载才清空。
- debug APK 约 **24.3MB**（material-icons + Room/DataStore + Navigation + Markdown renderer）。
- 已验证：四 Tab 切换/状态保留、历史列表、点击会话、新建会话、长按删除弹窗、重命名；整理为知识已实际写入
  Room（日志 `I KnowledgeOrganizer: Knowledge entry stored: id=1, conversationId=2, fallback=false`）。
- 知识库列表/搜索/详情、Markdown 详情与来源会话入口已完成并编译通过；真机当前安装的是 3.5.2，这些页面**尚未在真机上逐项打开验证**。
- release 仍未配置签名，`isMinifyEnabled = false`。

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
7. **GitHub 网络**：2026-10-31 关闭 Watt Toolkit 后仍无法连接 `github.com:443`（`Test-NetConnection` 返回 False，
   Git 无代理配置）。本地 commit `cc6721c` 完整，网络恢复后 `git push origin main`。GCM 在 Agent 沙箱内
   还可能因无法创建子进程失败；必要时用更高权限或普通终端推送。
8. **沙箱写 `.ssh` 被拒**：不要生成/配置 SSH 密钥；走 HTTPS + GCM。
9. **PowerShell 的 HTTPS 失败**：本沙箱内 `Invoke-WebRequest` / `curl.exe` 报 TLS 凭证错误，但 Gradle/JDK
   网络正常。查 Maven 版本或下载依赖请交给 Gradle，或写临时 Java 单文件程序，不要据此判定断网。
10. **Room/KSP 首次构建慢**：首次 `assembleDebug` 可能约 5 分钟，之后增量正常。
11. **Markdown 库体积**：`multiplatform-markdown-renderer-m3:0.46.0` 会引入 Compose Multiplatform 适配层，
    APK 从约 20.5MB 增至约 24.3MB；已由用户确认允许，仅此一个渲染库。
12. **HANDOFF / 截图策略**：HANDOFF 只在用户要求交接时更新；临时截图放 `build\tmp\`；不要每个任务自动装真机/截图。

---

## 6. Git 状态

- 分支 `main`，最新本地提交包含 4.5.1（上一提交 `cc6721c`），尚未推送。
- 远端：`https://github.com/super-star12345/recAIteLAB.git`（Public）。
- 已提交：全部源码、`CLAUDE.md`、`PRD.md`、plan 文档、Room schema、JUnit 测试、版本 4.5.1（本地）。
- 未提交：无（4.5.1 与本次 HANDOFF 已包含在本地提交）；`local.properties`、`build/`、APK、`.gradle/` 被忽略。
- 提交前已扫描源码无 `sk-*` 明文；不得提交 `local.properties` 或 debug APK。
- 网络恢复后：`git push origin main`。

---

## 7. 已知缺口 / 下一阶段

已完成的重点：

- Room 数据层（Provider / Conversation / Message / KnowledgeEntry）+ schema v1；
- Provider 配置、激活项、模型选择持久化；Keystore 加密 Key；DataStore 偏好；
- 会话/消息持久化、启动恢复、严格会话隔离（含 JUnit 回归测试）；
- 四 Tab 导航、历史记录（新建/打开/重命名/删除/整理为知识）、知识库列表/搜索/删除/详情/Markdown；
- 知识整理服务（JSON 解析 + 兜底落库）；
- temperature 默认不发送、IME 布局修复、版本 4.5.1。

阶段 5 打磨缺口：

- 对话页尚未用 `MarkdownText` 渲染助手消息；代码块复制未做。
- 上下文截断策略仍简单（整理输入首尾截断；聊天上下文未按 30 条/32k 限制）。
- 错误提示偏简单；`retry()` / `clear()` 无 UI 入口；停止的半截 assistant 不落库（需新增 `Message.status` + Migration）。
- 主题仅跟随系统，无多主题 / Material You；无自适应图标 / monochrome。
- 无 token 统计、思考链、多模态、联网搜索。
- 知识库未实现条目编辑、Markdown 导出、RAG 问答、批量/自动整理、整理专用模型。

阶段 6 测试与发布：

- 单测仅覆盖会话隔离；SSE 解析、上下文截断、知识 JSON 解析/兜底、Provider/模型拉取仍缺测试。
- release 未签名、未配置 minify；debug APK 内含真实 Key，禁止分发。
- 需真机冒烟 release 包并检查 `allowBackup=false`、权限与隐私说明。

建议下一入口：阶段 5 先接对话页 Markdown（复用 `ui/common/MarkdownText`）与代码块复制，再做上下文截断和错误提示；
最后补 release 签名与测试。

---

## 8. 约定提醒

- 严格遵循 `CLAUDE.md`：不改既定 SDK/AGP/Gradle/Compose BOM 版本；不加清单外依赖；不用 XML 布局；
  Key 不落日志/不进 Git；用户可见文案进 `strings.xml`；改完必须 `assembleDebug` 通过。
- 状态提升：Screen 只接收 state + 回调，不访问 Repository/ViewModel。
- 网络只走 OkHttp（未引入 Retrofit）；JSON 统一 kotlinx.serialization + `openAiJson`。
- 调试配置只从 `local.properties` 进 BuildConfig（debug）；业务代码不直接读 `BuildConfig.DEEPSEEK_*`。
- 当前版本 **4.5.1 / 451**；装真机前确认版本号（真机当前仍为 3.5.2）。
- 临时截图 / 调试产物统一放 `build\tmp\`（已 gitignore，`gradlew clean` 会清空）。
- 用户要求：**不要每个任务自动安装/打开/截图**；只有明确要求时才装真机。HANDOFF 也只在要求交接时更新。

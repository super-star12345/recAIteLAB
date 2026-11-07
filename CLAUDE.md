# CLAUDE.md — recAIteLAB 项目规则

> BYO-Key 聚合 AI 聊天 App：用户自带 Key，调用 OpenAI 兼容第三方 API，支持多模型切换。
> 本文件是本仓库的强制约定，与个人习惯冲突时以此为准。

## 1. 技术栈与构建（已锁定）

- Kotlin + Jetpack Compose（Material 3）+ MVVM + Room + DataStore + OkHttp/Retrofit + kotlinx.serialization。
- 包名 / applicationId / namespace：`com.recaitelab.app`，三者保持一致。
- AGP 9.4.1（**Kotlin 支持已内置，禁止再加 `org.jetbrains.kotlin.android`**）；Compose 编译器插件 2.4.21。
- Gradle 9.8.2（wrapper）、JDK 17、compileSdk 37 + `compileSdkMinor = 2`、targetSdk 36、minSdk 24、Compose BOM 2026.09.00。
- 依赖直接写在 `app/build.gradle.kts`（暂无 version catalog），不要擅自改成版本目录结构。
- 构建验证用 PowerShell：`.\gradlew.bat assembleDebug`；安装 `.\gradlew.bat installDebug`（真机 10AF5T05EL001BF）。
- 工具链在 `F:\AndroidToolchain`，**不要重装**；模拟器不可用，不要再尝试。环境坑见 `HANDOFF.md`。

## 2. 包结构（`com.recaitelab.app.*`）

```
com.recaitelab.app
├── MainActivity.kt
├── ui/
│   ├── chat/        # ChatScreen.kt / ChatViewModel.kt / ChatUiState.kt
│   ├── models/      # 模型列表、多模型切换
│   ├── settings/    # Key、Base URL、生成参数
│   ├── components/  # 可复用无状态组件
│   └── theme/       # Color / Type / Theme
├── data/
│   ├── repository/  # ChatRepository、SettingsRepository
│   ├── local/       # Room（entity / dao / db）+ DataStore
│   └── remote/      # Retrofit Api、DTO、拦截器
├── domain/model/    # 纯 Kotlin：Message、Conversation、ModelInfo、Provider
└── di/              # 依赖装配（Hilt 未引入前用手动 AppContainer）
```

- `ui/` 按页面分包，一页一包：`XxxScreen.kt` + `XxxViewModel.kt` + `XxxUiState.kt` 同包放置。
- `domain/model` 不依赖 Android、网络、数据库框架；DTO ↔ domain 映射只写在 `data` 层。
- `di` 统一提供单例（OkHttpClient、Retrofit、Room、DataStore），Composable 不得自行 new。

## 3. 编码规范

- Kotlin 官方风格：4 空格缩进、无通配 import、类 PascalCase、函数/属性 camelCase、常量 UPPER_SNAKE。
- 公开 API 显式声明返回类型；优先 `val` 与不可变数据类；状态用 `sealed interface` 表达。
- Compose：**State hoisting**——路由级 Composable 收集状态并下传，子 Composable 只接收 `state` + 事件 lambda。
- Composable 内禁止业务逻辑，禁止直接访问 Repository / ViewModel；只做布局与事件上抛。
- 可复用 Composable 提供 `modifier: Modifier = Modifier` 参数；列表用 `LazyColumn` 并给稳定 `key`。
- 每个页面提供 `@Preview`，用假数据构造状态，不在预览中发网络请求。
- ViewModel 只暴露 `StateFlow<UiState>`，用 `stateIn(viewModelScope, WhileSubscribed(5_000), 初始值)`。
- Repository：数据库读取返回 `Flow<T>`，网络请求为 `suspend fun`；IO 切 `Dispatchers.IO`。
- 禁止 `GlobalScope`、`runBlocking`、回调式 `enqueue`；一次性事件用 `Channel` / `SharedFlow`，不用 `LiveData`。
- 异常在 Repository 层收敛为 `Result` 或领域错误类型，不把原始 `Throwable` 抛给 UI。
- 用户可见文案放 `strings.xml`；JSON 统一 kotlinx.serialization，`Json { ignoreUnknownKeys = true }`。

## 4. 硬性约束（违反即回退）

- **未经确认不得引入任何新第三方依赖或 Gradle 插件**：先说明用途、版本、体积影响，等我明确同意后再改。
- **不得硬编码 API Key / Base URL / 任何密钥**：Key 存 DataStore，不写日志、不进 Git、不写死 `BuildConfig`。
- **不使用 XML 布局**，全部 Compose；XML 仅限 `values/` 资源与 `AndroidManifest.xml`。
- **所有网络请求必须走 `suspend` 函数或 `Flow`**，不得阻塞主线程、不得使用回调式异步。
- 不得替换既定技术栈或改动已定的 SDK / AGP / Gradle / Compose BOM 版本。
- 日志中不得输出 Authorization、完整请求体、用户对话内容。

## 5. 工作方式

- 动手前先列出**将要修改或新增的文件清单**及每个文件的目的，再进行改动。
- 单次改动聚焦一件事；不做顺手重构、不重命名无关文件、不整仓格式化。
- 每次改完必须保证可编译：执行 `.\gradlew.bat assembleDebug` 并确认成功；失败则修复或明确说明原因，不留半成品。
- 新功能按「UiState → Repository → 网络/存储 → UI」顺序小步落地，每一步保持可编译。
- 需求或交互不明确时先提问，不猜测产品行为。
- **不自动 `git push`**：阶段完成时本地 `git commit` 即可；推送（含首次 `-u`）只在用户明确要求时执行。
- **不自动更新 `HANDOFF.md`**：仅在用户明确说「交接 / 更新 handoff」时更新；其它任务不要把 HANDOFF 计入改动清单。
- 环境与工具链结论只写进 `HANDOFF.md`，不在本文件重复。

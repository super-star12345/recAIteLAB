# Prism Glass 主题改造基线（T0）

> 本文件记录 recAIteLAB 引入 Prism Glass 主题前的基线状态，对应设计拆解文档
> `prism-glass-theme-plan.md` 的 T0「改造准备与 Material 基线」阶段。
>
> 基线 commit：`f8d96e0`。文中行号均以该 commit 为准；后续改动请以文件内容为准重新核对。

## 1. 分支与提交

| 项 | 值 |
| --- | --- |
| 当前分支 | `feature/prism-glass` |
| 分支来源 | `main` |
| 基线 commit | `f8d96e06699db1580c1b5160eea163c08103c24c`（短 hash `f8d96e0`） |
| commit 说明 | `feat: compact markdown headings in chat bubbles` |
| 分支状态 | 本地新建，尚未推送（`origin/main` 仍在 `dfdbd54`） |
| 工作区 | 创建本文件前干净；本文件为 T0 新增，尚未提交 |
| 应用版本 | versionName `4.5.1` / versionCode `451` |

T0 之前的前置基线小修（对话页 Markdown 标题紧凑化）先提交在 `main`，避免与主题改动混进同一份 diff：

- `app/src/main/java/com/recaitelab/app/ui/common/MarkdownText.kt`：新增 `MarkdownTextStyle`（`ARTICLE` / `CHAT`）。
  `CHAT` 把 H1/H2 收敛到 `titleLarge` / `titleMedium` 量级，H3–H6 用 `bodyLarge` + 加粗。
- `app/src/main/java/com/recaitelab/app/ui/chat/ChatScreen.kt`：助手消息气泡传入 `style = MarkdownTextStyle.CHAT`。
- 知识详情继续使用默认的 `ARTICLE` 文章级排版。

## 2. 构建与测试基线

命令（Windows PowerShell，仓库根目录）：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

| 项 | 结果 |
| --- | --- |
| `testDebugUnitTest` | BUILD SUCCESSFUL，**52 个用例全部通过**（7 个测试类，0 failure / 0 error / 0 skipped） |
| `assembleDebug` | BUILD SUCCESSFUL |
| 产物 | `app/build/outputs/apk/debug/app-debug.apk`，约 20.9 MB（SHA-256 前缀 `BB91CE01A1139389`） |
| 环境 | Gradle 9.8.2（wrapper）、JDK 17、compileSdk 37.2 / targetSdk 36 / minSdk 24 |

测试类明细：

| 测试类 | 用例数 |
| --- | --- |
| `data.remote.ModelListParserTest` | 7 |
| `data.remote.SseChatStreamParserTest` | 7 |
| `data.remote.SseEventReaderTest` | 7 |
| `data.repository.ConversationRepositoryTest` | 3 |
| `data.repository.ErrorMapperTest` | 6 |
| `data.repository.KnowledgePayloadParserTest` | 10 |
| `domain.model.ContextWindowTest` | 12 |
| **合计** | **52** |

补充说明：

- 为排除增量缓存影响，另执行 `.\gradlew.bat :app:compileDebugKotlin --rerun`，Kotlin 源码从零重编译通过，
  随后 `assembleDebug` 重新打包成功（APK 时间戳为本次构建）。
- 构建日志中的 `File watcher server ... Couldn't open current thread, error = 5` 属 Windows 沙箱环境噪音，
  不影响构建结果（参见 `HANDOFF.md` 环境坑）。
- `HANDOFF.md` 中「`testDebugUnitTest` 3/3」是 2026-10-31 的旧快照，已过时；以本文件为准。

## 3. 当前 Material 3 主题实现

### 3.1 主题入口与色板

| 文件 | 作用 |
| --- | --- |
| `app/src/main/java/com/recaitelab/app/ui/theme/Theme.kt` | `RecAIteLABTheme(appearanceMode, colorTheme, dynamicColorEnabled)`：解析 SYSTEM/LIGHT/DARK → `darkTheme`；Android 12+ 且开关打开时用 `dynamicLight/DarkColorScheme`，否则按 `AppColorTheme` 选色板；最后套 `MaterialTheme(colorScheme, AppTypography)` |
| `app/src/main/java/com/recaitelab/app/ui/theme/Color.kt` | 6 套 `ColorScheme`：`LightColorScheme` / `DarkColorScheme`（青绿）、`GreenLight/GreenDark`（护眼绿）、`BlueLight/BlueDark`（商务蓝） |
| `app/src/main/java/com/recaitelab/app/ui/theme/Type.kt` | `AppTypography = Typography()`，M3 默认字阶，无自定义字体 |
| `app/src/main/res/values/themes.xml` / `values-night/themes.xml` | Android 窗口主题 `Theme.RecAIteLab`（`Material.Light.NoActionBar` / `Material.NoActionBar`），只影响启动窗口背景，不参与 Compose 配色 |
| `app/src/main/res/values/colors.xml` | 仅 `ic_launcher_background`（自适应图标底色） |

关键事实：

- `RecAIteLABTheme` **只设置 `colorScheme` 与 `typography`，未自定义 `shapes`**（使用 M3 默认形状）。
  Prism 需要新增独立形状系统，而不是覆盖 M3 `shapes`，否则会波及 Material 模式。
- `ui/theme/Color.kt` 是全项目唯一允许出现 `Color(0x...)` 的文件；页面组件一律取 `MaterialTheme.colorScheme`。
- 动态取色开启时会覆盖所选 `AppColorTheme`，基线截图必须关闭动态取色才能复现。

### 3.2 DataStore 主题偏好

| 文件 | 内容 |
| --- | --- |
| `app/src/main/java/com/recaitelab/app/domain/model/ThemePreferences.kt` | `enum AppearanceMode { SYSTEM, LIGHT, DARK }`、`enum AppColorTheme { DEFAULT, GREEN, BLUE }` |
| `app/src/main/java/com/recaitelab/app/data/local/preferences/UserPreferencesRepository.kt` | DataStore（`preferencesDataStore(name = "settings")`）读写主题偏好；读取 IO 异常回退 `emptyPreferences()` |
| `app/src/main/java/com/recaitelab/app/ui/settings/SettingsUiState.kt` | `appearanceMode` / `colorTheme` / `dynamicColorEnabled` / `isDynamicColorSupported` |
| `app/src/main/java/com/recaitelab/app/ui/settings/SettingsViewModel.kt` | `combine` 三个 Flow → UiState；`AppearanceModeChange` / `ColorThemeChange` / `DynamicColorChange` → `persistTheme`（DataStore 为唯一数据源，写入后 Flow 回流） |
| `app/src/main/java/com/recaitelab/app/ui/settings/SettingsScreen.kt` | 设置首页两个入口（外观 / 模型选择）+ 外观摘要 |
| `app/src/main/java/com/recaitelab/app/ui/settings/AppearanceSettingsScreen.kt` | 外观二级页：外观模式 RadioButton、配色方案 FilterChip、动态取色 Switch |

DataStore 主题键：

| 键 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `theme_mode` | String | `SYSTEM` | `AppearanceMode.name`，非法值回退 SYSTEM |
| `color_theme` | String | `DEFAULT` | `AppColorTheme.name`，非法值回退 DEFAULT |
| `dynamic_color_enabled` | Boolean | `false` | 仅 Android 12+ 生效 |

同 DataStore 中还有 `last_provider_id` / `last_model_id` / `last_conversation_id` / `default_provider_seeded`；
API Key 密文在独立 DataStore `secure_keys`（`data/local/security/ApiKeyStore.kt`），不在本文件范围内。

### 3.3 主题应用点（MainActivity）

| 位置 | 内容 |
| --- | --- |
| `app/src/main/java/com/recaitelab/app/MainActivity.kt:62-77` | `setContent`：Activity 作用域 `SettingsViewModel` → `collectAsState()` → `RecAIteLABTheme(appearanceMode, colorTheme, dynamicColorEnabled) { AppRoot(settingsViewModel) }` |
| `MainActivity.kt:85-200` | `AppRoot`：`rememberNavController()`、4 个 Activity 作用域 ViewModel、根 `Scaffold`、`NavHost` |
| `MainActivity.kt:354-367` | `topLevelTabs`：4 个一级 Tab 的 route / 文案资源 / Material 图标 |
| `MainActivity.kt:373-381` | `navigateToTab()`：`popUpTo(startDestination){saveState=true}` + `launchSingleTop` + `restoreState` |

主题切换不重启 Activity：偏好写 DataStore → Flow 回流 → `MainActivity` 重组 `RecAIteLABTheme`。
**Prism 的双主题切换应沿用同一路径**（新增 preference + 在 `setContent` 顶层分流），不要另起 Activity 或重建导航。

### 3.4 根 Scaffold

`MainActivity.kt:103-134`

- `Modifier.fillMaxSize()`
- `contentWindowInsets = WindowInsets(0, 0, 0, 0)`：状态栏由各页 `TopAppBar` 处理，导航栏由常驻
  `NavigationBar` 处理，外层不重复加 insets。
- `bottomBar = { if (showBottomBar) NavigationBar { ... } }`
- `showBottomBar`（`MainActivity.kt:91-101`）：当前 destination 的 `hierarchy` 命中 4 个一级 Tab 之一才显示；
  知识详情、外观、模型选择等二级页隐藏。**键盘显隐不参与判断**。

### 3.5 NavigationBar

`MainActivity.kt:107-133`（M3 `NavigationBar` + `NavigationBarItem`）

- 4 个 item 来自 `topLevelTabs`，点击调用 `navigateToTab(tab.route)`。
- 选中判定：`currentDestination.hierarchy` 中任一 destination 与 tab route 的 class 匹配；
  `currentDestination` 为 null 时回退 `ChatRoute` 选中。
- 底栏属于根 `Scaffold` 的 `bottomBar`，始终在布局内，不随 IME 显隐、不做高度动画、不消费 IME insets。
  这是 `HANDOFF.md` 记录的真机 IME 修复，**Prism 改造不得破坏**。

### 3.6 NavHost 与路由

`MainActivity.kt:135-198`：`startDestination = ChatRoute`，
`Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)`。

| 路由（`ui/navigation/AppRoutes.kt`） | 层级 | 目的地 |
| --- | --- | --- |
| `ChatRoute` | Tab · AI 对话 | `ChatScreen` |
| `HistoryRoute` | Tab · 历史记录 | `HistoryScreen` |
| `KnowledgeRoute` | Tab · 知识库 | `KnowledgeScreen` |
| `SettingsRoute` | Tab · 设置 | `SettingsScreen` |
| `KnowledgeDetailRoute(entryId: Long)` | 二级 | `KnowledgeDetailScreen` |
| `SettingsAppearanceRoute` | 二级 | `AppearanceSettingsScreen` |
| `SettingsModelsRoute` | 二级 | `ModelSettingsScreen`（含 Provider 添加/编辑表单） |

- 路由均为 `@Serializable` 类型安全对象/数据类，无手写字符串 route。
- 一级 Tab 切换保留各自返回栈与状态；二级页 push 使用 Navigation 默认转场（当前无自定义动画）。
  T2 只替换 Prism 模式的转场，Material 模式必须保持现状。

### 3.7 页面级 Scaffold 与 IME（改造时必须保留）

| 文件 | 现状 |
| --- | --- |
| `ui/chat/ChatScreen.kt:122-203` | 内层 `Scaffold` 只有 `topBar`，`contentWindowInsets = 0`；content 为 `Column`：消息区 `Box(weight(1f))` + 固定 `ChatInputBar`；`imePadding()` 只在 `ChatInputBar`（:200）出现一次 |
| `ui/settings/ModelSettingsScreen.kt:88` | 表单根保留一次 `imePadding()` |
| `ui/history/HistoryScreen.kt` | `TopAppBar` + FAB 新建 + 长按删除 / 溢出菜单（重命名、删除、整理为知识） |
| `ui/knowledge/KnowledgeScreen.kt` | `TopAppBar` + `OutlinedTextField` 搜索 + 条目卡片 |
| `ui/knowledge/KnowledgeDetailScreen.kt` | `TopAppBar` + `MarkdownText`（ARTICLE 样式） |
| `ui/common/MarkdownText.kt` | 使用 `markdownColor()` / `markdownTypography()`，颜色来自 M3 `MaterialTheme`；Prism 模式需单独提供 Markdown 配色，不能改全局 M3 Typography |

---

## 4. 后续改造范围

### T1：双主题架构 + Prism 静态预览

- 新增 `AppDesignSystem { MATERIAL_3, PRISM_GLASS }`，默认 `MATERIAL_3`；老用户升级后界面与基线一致。
- DataStore 新增：`design_system`、`prism_color_style`（默认 SOFT）、`prism_seed_color`、`glass_quality`（默认 STANDARD）。
- 新包 `ui/prism/`：`PrismTheme.kt`（CompositionLocal + 入口）、`PrismColors.kt`（种子色 + vivid/tinted/soft/monochrome × 明暗）、
  `PrismShapes.kt`（连续圆角/超椭圆近似）、`PrismMotion.kt`（弹簧与按压缩放）、`PrismComponents.kt`、
  `PrismNavigation.kt`、`glass/`（`GlassStyle`、`StandardGlass`、`UltraGlass`）。
- 设置页增加「界面风格」，Prism 预览覆盖卡片、列表行、按钮、顶栏、底栏、选中态与深浅色。
- 主题对象缓存，避免无意义重建；切换主题不重启 Activity、不丢数据。
- 本阶段不启用页面转场与背景模糊，不修改正式业务页面。

### T2：页面转场原型（先单个详情页）

- 新页从右侧进入，被覆盖页联动位移约屏宽 30%、叠加约 10% 暗化。
- 约 300ms 临界阻尼弹簧，返回采用对称运动；尊重系统动画缩放，关闭动画时降级。
- 只作用于 Prism 模式；Material 模式导航与动画完全不变。
- 可选（不阻塞验收）：屏幕圆角裁剪、recede 缩放（最小约 0.8）、手势返回。

### T3：标准 Liquid Glass 原型（局部）

- 先只选底栏 / 顶栏 / Sheet 三者之一验证：背景模糊 + 半透明 tint + 细边缘高光 + 轻微阴影。
- 优先复用成熟的 Compose backdrop blur 方案，首版不从零实现全屏背景捕获。
- 不支持时降级为 frost / tint / 描边；`OFF` 时使用普通不透明表面。
- 不在 `LazyColumn` 的每个 item 或消息气泡上创建模糊层；同组共享采样、静止复用快照。
- 检查滚动、转场、键盘弹出时的性能与布局；不满意停留本阶段调参，不进入 T4。

### 范围边界

| 分类 | 内容 |
| --- | --- |
| 必做 | 四种配色、连续圆角、push 联动转场、标准玻璃、统一按压反馈 |
| 可选 | Ultra Glass（Android 13+ AGSL 折射/色散）、手势返回、goo 融合、屏幕圆角 |
| 不做 | Flutter 重写、桌面 sidebar、完整复刻 prism_ui 组件、修改业务逻辑 |

---

## 5. 约束

1. **不引入 Flutter**：全部用原生 Compose 实现；也不引入新的 UI/玻璃/动画第三方库（确需引入须先说明用途、版本、体积并等确认）。
2. **不删除 Material 3**：`ui/theme/`、现有 Material 页面与组件全部保留；默认仍是 `MATERIAL_3`，老用户升级后界面不变，Prism 可随时回退。
3. **不修改 ViewModel / Repository / Room / 网络层**：`ui/chat`、`ui/history`、`ui/knowledge`、`ui/settings` 的 ViewModel 与 `data/` 层不改行为；
   仅允许在 Composable 层做视觉容器分流。
4. **不复制两套业务逻辑**：两套主题共享同一 `UiState` 与同一事件回调；禁止复制 Screen 内的业务分支或状态。
5. 视觉分流只在 Composable 层：Material / Prism 两套容器接收同样的 `state` + lambda。
6. 不使用 XML 布局（仅 `values/` 资源与 `AndroidManifest.xml`）；不改包名 / applicationId / namespace。
7. 不改动既定的 SDK、AGP、Gradle、Compose BOM 版本；不新增依赖目录结构。
8. 保留 IME 行为：根 `Scaffold` insets 为 0、底栏常驻、`ChatInputBar` 单次 `imePadding()`；
   玻璃底栏不得引入额外的 IME 显隐动画或高度变化。
9. 颜色与字号继续走 Token：Prism 色板集中在新文件，正式页面不得硬编码 `Color(0x...)`。
10. 每个阶段结束必须：`assembleDebug` 通过 → 真机验收 → commit → 再进入下一阶段；禁止跨阶段并行推进页面、转场与 Ultra Shader。

---

## 6. 基线截图（T0 验收，已完成）

状态：**10 张全部完成**（2026-11-07，真机，已按第 7 节修复后重拍）。文件位于 `docs/baseline/screenshots/`，
完整 1440×3168，保留系统状态栏与手势区。

> 这批截图对应修复后的 build（代码 `cf8a74a`，真机 21:01 安装）：状态栏图标明暗、容器配色、
> 历史预览与 tags 换行均已是修复后状态；修复前的旧图不再保留。

| 编号 | 页面 | 模式 | 文件 | 状态 |
| --- | --- | --- | --- | --- |
| SC-01 | AI 对话 | 浅色 | [SC-01-chat-light.png](screenshots/SC-01-chat-light.png) | ✅ |
| SC-02 | AI 对话 | 深色 | [SC-02-chat-dark.png](screenshots/SC-02-chat-dark.png) | ✅ |
| SC-03 | 历史记录 | 浅色 | [SC-03-history-light.png](screenshots/SC-03-history-light.png) | ✅ |
| SC-04 | 历史记录 | 深色 | [SC-04-history-dark.png](screenshots/SC-04-history-dark.png) | ✅ |
| SC-05 | 知识库 | 浅色 | [SC-05-knowledge-light.png](screenshots/SC-05-knowledge-light.png) | ✅ |
| SC-06 | 知识库 | 深色 | [SC-06-knowledge-dark.png](screenshots/SC-06-knowledge-dark.png) | ✅ |
| SC-07 | 设置 | 浅色 | [SC-07-settings-light.png](screenshots/SC-07-settings-light.png) | ✅ |
| SC-08 | 设置 | 深色 | [SC-08-settings-dark.png](screenshots/SC-08-settings-dark.png) | ✅ |
| SC-09 | 知识详情 | 浅色 | [SC-09-knowledge-detail-light.png](screenshots/SC-09-knowledge-detail-light.png) | ✅ |
| SC-10 | 知识详情 | 深色 | [SC-10-knowledge-detail-dark.png](screenshots/SC-10-knowledge-detail-dark.png) | ✅ |

拍摄环境：

| 项 | 值 |
| --- | --- |
| 设备 | `10AF5T05EL001BF`（vivo V2463A / Android 17 / SDK 37），1440×3168，density 640 |
| APK | debug `4.5.1 / 451`，代码 commit `cf8a74a`（2026-11-07 21:01 安装，数据保留） |
| 主题 | 外观模式「浅色」/「深色」（App 内切换）、配色方案「默认」、动态取色「关闭」 |
| 深浅切换 | 设置 → 外观 → 浅色 / 深色（不依赖系统夜间模式） |
| 截图方式 | `adb shell screencap`，保留完整系统栏（1440×3168），未裁剪 |
| 会话/数据 | 会话 12（滴定操作，含 H3 标题/加粗/列表/行内公式）；历史 8 条；知识 3 条（含 tags）；Provider DeepSeek + KIMI |

与清单的差异：

- 对话截图（SC-01/02）使用已有中文会话，含 H3 标题、加粗、无序/嵌套列表与行内公式，
  **不含代码块**；代码块样式在 SC-09/10（九九乘法表）中覆盖。
- 浅色/深色均为 App 内固定外观模式，状态栏图标随主题正确切换（修复前白底白图标问题已解决）。

---

## 7. 基线发现的现有问题（与 Prism 无关，改造前记录）

> 2026-11-07 更新：问题 1–4 已修复（构建与 58 个单测通过，用户人工验证通过），截图已按修复后重拍。
> 本节保留原始现象记录，便于对照回归。

以下为 T0 真机截图过程中观察到、**当时 Material 3 版本已存在**的问题。Prism 阶段不顺手修复，
但改造涉及这些组件时需要避免继承，或在 T1/T2 前单独决策。

| # | 现象 | 影响 | 初步定位 | 状态 |
| --- | --- | --- | --- | --- |
| 1 | 浅色模式状态栏图标为白色，时钟/信号几乎不可见；深色正常 | 浅色可用性差 | App 未通过 `WindowInsetsController` 设置 light status bar appearance；targetSdk 36 强制 edge-to-edge 后仅靠 `Theme.Material.Light` 不再生效 | ✅ 已修复（`Theme.kt` SideEffect 设置系统栏图标明暗） |
| 2 | `NavigationBar` 底部、知识库卡片、FAB 呈紫色调，与青绿主色不一致 | 品牌色不统一 | `Color.kt` 只覆盖旧版 M3 角色，未设置 `surfaceContainer*` / `surfaceContainerHigh` 等新角色，组件回落到 M3 默认紫色调色板 | ✅ 已修复（6 套色板补齐 `surfaceBright/Dim` 与 5 档 `surfaceContainer*`） |
| 3 | 历史记录预览直接显示 Markdown 源码（`**加粗**`、`###`、```` ``` ````） | 列表可读性差 | `HistoryScreen` 预览未剥离 Markdown 标记 | ✅ 已修复（`ui/common/MarkdownPreview.kt` + 单测，`HistoryViewModel` 调用） |
| 4 | 知识库卡片 tags 与时间同行，tags ≥4 时最后一个 tag 被时间覆盖/裁切 | 标签不可读 | `KnowledgeScreen` 卡片行布局未换行/限宽 | ✅ 已修复（`FlowRow` 换行，时间与 tags 分列） |
| 5 | 聊天气泡中的 LaTeX（`\[ ... \]`）按纯文本显示 | 公式不可读 | Markdown 渲染器不支持 LaTeX，属预期限制 | 保留 |
| 6 | 历史页 FAB 覆盖列表最后一条内容 | 轻微遮挡 | M3 FAB 常规行为，可接受 | 保留 |

原建议：问题 1、2 与 Prism 的「玻璃底栏 / 状态栏沉浸」直接相关，进入 T2/T3 前需先决定是修复还是
在 Prism 模式内单独处理；问题 3、4 可作为 T4 铺开时的顺带视觉修复项（均已在 T0 后修复）。


# PRD — recAIteLAB（MVP）

> 依据 `android-ai-chat-vibe-coding-plan.md`（v2）修订：四 Tab 导航、知识库、Provider 抽象与七阶段计划。

**定位**：Android 聚合 AI 聊天 App「recAIteLAB」（`com.recaitelab.app`）。BYO-Key，用户自填 API Key 与 Base URL，直连 OpenAI 兼容接口；支持多 Provider、多模型切换；对话历史可经 AI 整理为个人知识库。无账号、无自建后端、不代理用户流量。

**全局约束**

- 技术栈：Kotlin + Compose（M3）+ MVVM + Room + DataStore + OkHttp + kotlinx.serialization + Navigation Compose；包结构与编码规范见 `CLAUDE.md`。
- 技术栈内依赖可直接接入；清单外的新库或插件仍需确认。Markdown 自研最小子集，不引库；DI 用手写 `di/AppContainer`，Hilt 未引入。
- 网络层按 `ChatProvider` 抽象：`streamChat(messages, model, temperature): Flow<String>`、`fetchModels(): List<String>`；MVP 仅实现 `OpenAiCompatibleProvider`，`Provider.type` 预留 Gemini / Anthropic 适配。
- Key 仅以密文落盘：Keystore AES-GCM 加密后存 DataStore；Room 不存明文；调试 Key 只放 `local.properties`，不进源码、不提交、不发给 Agent。
- 颜色与字号一律取 `MaterialTheme.colorScheme` / `typography`，禁止硬编码。

P0 = MVP 必须；P1 = MVP 打磨项；P2 = 二期。

## 1. 功能清单

| 模块 | 功能点 | 优先级 |
| --- | --- | --- |
| 对话 | 多轮上下文聊天（system + 最近 30 条 / ≤32k 字符） | P0 |
| 对话 | SSE 流式输出，可中断 | P0 |
| 对话 | Markdown 渲染（标题/列表/粗斜体/行内代码/代码块/引用/链接） | P0 |
| 对话 | 停止生成（保留已输出内容） | P0 |
| 对话 | 重新生成最后一条回复 | P0 |
| 对话 | 复制消息 / 代码块 | P1 |
| 对话 | 思考链展示、Token 用量统计 | P2 |
| 对话 | 联网搜索、多模态输入 | P2 |
| 会话 | 新建 / 重命名 / 删除会话 | P0 |
| 会话 | Room 本地历史持久化 | P0 |
| 模型 | Provider 增删改（名称 / 类型 / Base URL / Key / 温度） | P0 |
| 模型 | `GET /models` 动态拉取模型列表 | P0 |
| 模型 | 会话内切换 Provider 与模型 | P0 |
| 知识库 | 选中历史会话，调用模型整理为知识条目 | P0 |
| 知识库 | JSON 解析失败兜底（原文保留） | P0 |
| 知识库 | 条目列表 / 详情（Markdown）/ 搜索 / 删除 | P0 |
| 知识库 | 从条目跳转来源会话 | P0 |
| 知识库 | 条目编辑、Markdown 导出 | P2 |
| 知识库 | 批量 / 自动整理、超长对话分段整理 | P2 |
| 知识库 | 知识库问答（RAG-lite） | P2 |
| 知识库 | 整理专用模型设置 | P2 |
| 配置 | Base URL / API Key / 温度 | P0 |
| 配置 | Key 加密存储与掩码显示 | P0 |
| 配置 | Material 3 深浅色主题 | P0 |
| 配置 | 多主题切换、Material You 动态取色 | P1 |
| 导航 | 四 Tab 底栏（AI 对话 / 历史记录 / 知识库 / 设置） | P0 |
| 通用 | 统一错误提示（Key 失效 / 断网 / 限流） | P0 |
| 通用 | 自适应应用图标（含 monochrome） | P1 |
| 通用 | 空态引导（无 Provider / 无模型 / 无会话 / 无知识） | P1 |

## 2. 页面结构

### 2.1 导航骨架

- 单 Activity + `NavHost`；四个一级目的地对应底部 `NavigationBar`，二级页走导航栈 push，不显示底栏。
- 阶段 1–2 先做单聊天页；阶段 3 一次性接入四 Tab（知识库 / 设置先占位），避免后期返工。
- 路由使用 `@Serializable` route 对象（Navigation Compose 类型安全 API）；切换 Tab 用 `saveState/restoreState` + `launchSingleTop` + `popUpTo(startDestination)`，保留各 Tab 栈与滚动位置。

| 路由 | 层级 | 说明 |
| --- | --- | --- |
| `ChatRoute(conversationId: Long?)` | Tab · AI 对话 | 当前会话；id 为空显示空态 |
| `HistoryRoute` | Tab · 历史记录 | 会话列表 |
| `KnowledgeRoute` | Tab · 知识库 | 知识条目列表 |
| `SettingsRoute` | Tab · 设置 | Provider / 模型 / 主题 / API 参数 |
| `KnowledgeDetailRoute(entryId: Long)` | 二级 | 知识详情 |
| `ProviderEditRoute(providerId: Long?)` | 二级 | 新增 / 编辑 Provider |
| `ModelPickerRoute(conversationId: Long?)` | 二级 | 模型选择（对话内切换 / 模型管理） |

### 2.2 AI 对话 `ChatScreen`

- 空态：无会话时显示「新建对话」；无 Provider 或模型时按钮直达设置页。
- 顶栏：会话标题、当前「Provider · Model」选择器（点击 → 模型选择）、更多（重命名 / 整理为知识 / 删除会话）。
- 消息区：用户右、助手左；助手内容 Markdown 渲染；流式尾部显示光标；默认滚到底，用户上滑后暂停并显示「回到底部」。
- 发送：写 user 消息 → 写 assistant 消息（`STREAMING`）→ SSE 增量更新 → `COMPLETE` / `ERROR`；`POST {baseUrl}/chat/completions`，`stream=true`，逐行解析 `data:` 与 `[DONE]`。
- 停止 / 重新生成：取消协程与 HTTP 调用，保留已输出内容置 `STOPPED`；重新生成仅针对最后一条 assistant 消息，保留用户消息。
- 切换模型：仅影响后续请求；历史消息按 `modelName` 快照展示。
- 生成中切 Tab 继续（ViewModel 随返回栈存活）；主动停止或删除会话才取消。

### 2.3 历史记录 `HistoryScreen`

- 列表：标题、最后一条消息预览、相对时间；按 `updatedAt` 倒序；FAB 新建会话。
- 点击会话 → 以 `ChatRoute(conversationId)` 在对话 Tab 打开。
- 每条会话提供「整理为知识」入口（溢出菜单）；另支持重命名（1–40 字）、删除（二次确认，级联消息，生成中先停止）。
- 整理流程：确认框显示所用模型 → Loading 对话框（禁止重复触发）→ 成功 Snackbar「已存入知识库 · 查看」；失败提示重试。
- 整理请求：复用 `ChatProvider.streamChat` 收集完整文本（不逐字渲染），temperature 0.2；输入超 20k 字符截断并提示；使用会话当前 Provider / 模型，无绑定则用上次使用或首个可用模型，均无则先引导配置。
- 整理 Prompt 模板：

```text
你是知识整理助手。请阅读以下对话记录，提炼其中有长期价值的知识，只输出 JSON，不要输出其他内容：
{"title":"知识标题","summary":"一句话摘要","content":"Markdown 正文","tags":["标签1","标签2"]}
要求：去除寒暄与重复；保留结论、方法、代码片段、踩坑经验；正文用 Markdown 分节整理；tags 不超过 5 个。
对话记录：
【按 role: content 逐条拼接】
```

### 2.4 知识库 `KnowledgeScreen`

- 列表：标题、摘要（≤2 行）、标签 chips、来源标识、更新时间；按 `updatedAt` 倒序。
- 搜索：标题 / 摘要 / 正文 LIKE，输入防抖 300ms；空结果给提示。
- 点击 → 知识详情；长按或溢出菜单删除（二次确认）。
- 兜底条目显示「格式异常 · 已保留原文」标记；详情页可跳回来源会话（若会话已删除则隐藏入口）。
- 空态：引导「在历史记录中选择会话整理」。

### 2.5 设置 `SettingsScreen`

- 主题：跟随系统 / 浅色 / 深色，写 DataStore 并立即重组；P1 增加多主题与 Material You 动态取色（API 31+）。
- Provider 列表：名称、类型、Base URL、Key 掩码、模型数、启用开关；点击进入编辑，「添加 Provider」新建。
- 模型配置：进入模型选择页的管理模式，可启用 / 隐藏模型并查看上次使用。
- API 参数：温度在 Provider 编辑页配置；关于与版本号。
- 全部颜色取 `colorScheme`，不写死色值，保证深浅色与动态取色下可读。

### 2.6 二级页面

- **知识详情**：标题、标签、摘要、Markdown 正文（长文滚动、代码块横向滚动）、来源会话跳转、删除；编辑 / 导出（P2）。
- **Provider 编辑**：名称、类型（默认 openai_compatible）、Base URL（必填、仅 https、去尾 `/`）、API Key（密码框 + 可见切换，编辑时留空 = 不修改）、温度滑杆；「拉取模型」顺带验证连通性；删除（级联 Model + 清 DataStore 密钥）；未保存离开确认。
- **模型选择**：按 Provider 分组、搜索、单选；进入时刷新 `/models`，失败降级本地缓存；选中立即返回并写回会话 `providerId` / `modelId`；管理模式可启用 / 隐藏模型。

## 3. 数据模型

Room 数据库 `recaitelab.db`。时间统一 `Long`（epoch millis）；枚举以 `String` 存储 + `TypeConverter`，不用 ordinal。

### 3.1 `provider`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键自增 |
| name | String | 显示名，唯一 |
| baseUrl | String | 根地址，如 `https://api.openai.com/v1`，无尾斜杠 |
| type | String | `openai_compatible` / `gemini` / `anthropic`；MVP 仅实现第一种，其余预留 |
| temperature | Float | 0.0–2.0，默认 0.7 |
| isEnabled | Boolean | 停用后不参与模型选择 |
| createdAt / updatedAt | Long | 创建 / 更新时间 |

### 3.2 `model`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键自增 |
| providerId | Long | FK → `provider.id`，CASCADE，建索引 |
| modelId | String | 接口返回的 id，如 `gpt-4o-mini` |
| displayName | String | 展示名，默认等于 `modelId` |
| capabilities | String | JSON 数组：`text` / `vision` / `reasoning`；MVP 仅存储，二期使用 |
| isEnabled | Boolean | 是否出现在选择器 |
| lastSeenAt | Long | 最近一次拉取时间 |

唯一索引 `(providerId, modelId)`；重复拉取按该键 upsert，不删除旧行。

### 3.3 `conversation`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键自增 |
| title | String | 默认「新对话」 |
| providerId | Long? | 当前 Provider，FK → `provider.id`，SET_NULL |
| modelId | Long? | 当前模型，FK → `model.id`，SET_NULL |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 列表排序依据，发消息时更新 |

### 3.4 `message`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键自增 |
| conversationId | Long | FK → `conversation.id`，CASCADE，建索引 |
| role | String | `user` / `assistant` / `system` |
| content | String | 正文；流式期间原地更新 |
| status | String | `STREAMING` / `COMPLETE` / `STOPPED` / `ERROR` |
| tokenUsage | Int? | 接口返回 usage 时记录总 tokens；流式需 `stream_options.include_usage`，不支持则 null |
| modelName | String? | 生成时的模型快照，避免改名影响历史 |
| errorMessage | String? | 失败原因（已脱敏） |
| createdAt | Long | 发送时间，同会话内排序 |

### 3.5 `knowledge_entry`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键自增 |
| title | String | 整理标题，≤60 字；兜底为「未命名整理」 |
| summary | String | 一句话摘要，≤200 字 |
| content | String | Markdown 正文 |
| tags | String | JSON 数组（TypeConverter ↔ `List<String>`），MVP 用 LIKE 检索 |
| sourceConversationId | Long? | FK → `conversation.id`，SET_NULL（删会话不删知识） |
| sourceProviderId | Long? | 整理所用 Provider，FK → `provider.id`，SET_NULL |
| sourceModelName | String? | 整理所用模型快照 |
| parseStatus | String | `OK` / `FALLBACK`，兜底条目标记 |
| createdAt / updatedAt | Long | 创建 / 更新时间 |

- 关系：Provider 1—N Model；Conversation N—1 Provider/Model（可空）；Conversation 1—N Message；Conversation 1—N KnowledgeEntry。
- 索引：`model(providerId, modelId)` 唯一；`message(conversationId, createdAt)`；`knowledge_entry(updatedAt)`；`knowledge_entry(sourceConversationId)`。
- 密钥：`provider` 表不存 Key；密文以 `api_key_<providerId>` 存入 DataStore `secure_keys`，格式 `Base64(iv):Base64(cipher)`；删除 Provider 时先删库再删密钥。
- DataStore 其他键：`theme_mode`、`last_provider_id`、`last_model_id`。
- 迁移：导出 Room schema，版本升级必须写 `Migration`；开发期可临时 destructive，发布前移除。

## 4. 非功能需求

### 性能

- 首 token 目标 ≤2s（网络正常）；流式只更新最后一条消息，不整体刷新列表。
- Room、OkHttp、加解密、DataStore 全部 `Dispatchers.IO`；主线程零阻塞。
- 知识整理独立协程 + Loading 态，失败可重试；单次 90s 超时，响应上限 1MB。
- 消息分页 50 条/页、知识列表 20 条/页（P1）；聊天上下文 30 条 / 32k 字符，整理输入 20k 字符。
- 搜索防抖 300ms；列表使用稳定 key，避免重组抖动。
- SSE 解析、上下文截断、知识 JSON 解析与兜底抽成纯函数，供阶段 6 单测覆盖。

### 错误处理

- 状态码映射：401/403 → Key 无效或无权限；404 → Base URL/路径错误；429 → 频率超限；5xx → 服务端错误；`IOException`/超时 → 网络异常；400 → 透传 `error.message`。
- 非 2xx 读取响应体中的 `error.message`，截断后展示，不展示原始 HTML。
- SSE 中断：保留已生成内容，置 `STOPPED` / `ERROR` 并提供重试；不无限自动重连。
- **知识整理 JSON 兜底**：先剥 `json` 代码围栏，再取首个 `{` 到末个 `}` 的子串 → `Json { isLenient = true; ignoreUnknownKeys = true }` 解析 → 仍失败时以原文建条目：`title` = 「未命名整理」、`summary` = 原文前 100 字、`content` = 原始输出、`tags` = 空、`parseStatus = FALLBACK`，并标记「格式异常 · 已保留原文」，**绝不丢失内容**。
- 字段级兜底：content 为空用原文；tags 非数组置空；title / summary 超长截断；空响应或模型拒答同样走 FALLBACK。
- 整理中禁止重复触发；网络失败不建条目，保留会话与重试入口。
- 启动恢复：所有 `STREAMING` 消息置为 `STOPPED`，避免永久转圈。

### 安全

- Key 加密：Keystore AES-256-GCM（别名 `recaitelab_api_key_v1`）+ 随机 IV；明文仅在请求组装时短暂驻留内存。不引入 EncryptedSharedPreferences（已废弃且属新增依赖）。
- `android:allowBackup="false"`，避免密文随备份迁移后无法解密或泄露。
- 调试用真实 Key 只放 `local.properties`（已 gitignore），不写进源码、不提交、不发给 AI Agent。
- 日志脱敏：不打印 Key、Authorization、完整对话与知识正文；调试日志受 `BuildConfig.DEBUG` 控制。
- 仅允许 `https://` Base URL；禁用明文 HTTP（debug localhost 例外，P2）；敏感请求不跟随跨域重定向。
- 知识库仅存本机；只有用户手动触发整理时，对话内容才发送给当前 Provider，隐私边界与聊天一致；无统计上报与云同步。

## 5. 阶段划分

| 阶段 | 交付内容 | 验收 |
| --- | --- | --- |
| 0 环境与规范 | 工具链 + 真机 adb、包名重构、`CLAUDE.md`、本 PRD | `assembleDebug` 通过，真机可安装 |
| 1 最小可用对话 | 数据模型、SSE 客户端（BufferedSource 逐行）、Repository、ViewModel、M3 聊天页 + 主题、Key 配置（仅内存联调） | 逐字流式、停止生效、多轮上下文正确、深浅色跟随系统 |
| 2 多 Provider 与模型切换 | `ChatProvider` 抽象落地、Provider 管理 UI、动态拉取模型、对话页模型选择器 | 两个厂商 Key 来回切换对话正常 |
| 3 Room + 四 Tab | 五张表（`knowledge_entry` 先建空表）、历史记录 Tab、四 Tab 导航骨架、Key 加密存储 | 杀进程重进后历史与配置仍在；四 Tab 切换正常 |
| 4 知识库 | 整理入口、Prompt、收集完整输出、JSON 解析与兜底、落库、列表 / 详情 / 搜索 / 删除、详情 Markdown | 整理后出现条目、详情可看、重启仍在、解析失败不丢内容 |
| 5 体验打磨 | 对话页 Markdown、代码块复制、错误提示、上下文截断策略、多主题 / Material You、自适应图标 | 断网 / 错 Key / 长对话提示友好；无硬编码颜色；图标各遮罩不裁切 |
| 6 测试与发布 | 单测（SSE 解析、截断、知识 JSON 解析与兜底）、release 签名包、真机冒烟、上架合规 | release 包安装运行正常 |
| 二期 | 知识库问答（RAG-lite）、条目编辑 / 导出、批量 / 自动整理、超长对话分段、整理专用模型、思考链展示、联网搜索、多模态输入、Token 统计 | — |

- Key 在阶段 1–2 仅内存持有用于联调，阶段 3 完成 Keystore 加密落盘；任何阶段都不得明文持久化或提交。
- 阶段 1–2 只做单聊天页；四 Tab 在阶段 3 一次性接入。Markdown 渲染器阶段 4 先用于知识详情，阶段 5 接入对话页。
- MVP 完成标准：真机完成「配置 Provider → 拉取模型 → 对话 → 整理为知识 → 搜索并查看 → 杀进程重启后数据仍在」全流程，`assembleDebug` 与 release 包均可用。
- 纯本地 BYO-Key，分发型产品的后端转发 / 鉴权 / 计量不在本 PRD 范围。
- 不做（MVP）：账号 / 多端同步、图片与语音多模态、联网搜索、服务端代理、计费、多语言、平板适配。

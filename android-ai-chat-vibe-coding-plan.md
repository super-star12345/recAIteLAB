# recAIteLAB · Vibe Coding 开发总方案（v2 · 含知识库）

> 目标：通过 vibe coding 开发 Android 聚合 AI 聊天 App「recAIteLAB」。
> BYO-Key 调用第三方 API 聊天，支持多模型切换；对话历史可经 AI 整理为知识库。
> 本版新增：知识库功能、四页面底栏导航；已并入 Material 3、真机 adb、包名 com.recaitelab.app 等既定决策。

## 一、项目定位与核心功能

**产品形态**：Android 原生 App，BYO-Key 模式。用户自填 API Key/Base URL，在一个 App 内与多模型对话，并将有价值的对话沉淀为个人知识库。

**MVP 功能清单**：

| 模块 | 功能点 |
|---|---|
| 对话 | 多轮聊天、SSE 流式输出、Markdown 渲染、停止生成、重新生成 |
| 会话管理 | 新建/删除/重命名会话、本地历史持久化（历史记录 Tab） |
| 模型管理 | 多 Provider 增删改、GET /models 动态拉取模型列表、对话中切换模型 |
| **知识库** | **选中历史会话 → AI 整理为知识条目（标题/摘要/正文/标签）；知识库 Tab 展示列表+详情、搜索、删除** |
| 配置 | 自定义 Base URL/API Key/温度，Key 加密存储；Material 3 深浅色主题 |

**二期可选**：知识库问答（RAG-lite）、知识条目编辑/导出 Markdown、批量/自动整理、超长对话分段整理、整理专用模型设置、思考链展示、联网搜索、多模态输入、Token 统计。

## 二、页面结构与导航（四 Tab）

Navigation Compose + Material 3 NavigationBar 底栏：

| Tab | 内容 | 引入阶段 |
|---|---|---|
| AI 对话 | 聊天页：消息流、输入框、顶部模型选择器、停止/重新生成 | 阶段 1 |
| 历史记录 | 会话列表：新建/删除/重命名；每条会话带「整理为知识」入口 | 阶段 3 |
| 知识库 | 知识条目列表（标题/摘要/标签/时间）、详情页（Markdown）、搜索、删除 | 阶段 4 |
| 设置 | Provider 管理、模型配置、主题切换、API 参数 | 阶段 2 起充实 |

- 阶段 1–2 为单聊天页；**阶段 3 一次性搭好四 Tab 导航骨架**，知识库/设置可先占位
- 知识库详情、Provider 编辑等二级页面走导航栈 push，不进底栏

## 三、技术选型

| 层 | 选型 | 理由 |
|---|---|---|
| 语言/UI | Kotlin + Jetpack Compose（Material 3） | AI 生成质量最高；Material 风格既定 |
| 架构 | MVVM（ViewModel + StateFlow + Repository） | 结构规整，AI 不易跑偏 |
| 导航 | Navigation Compose + NavigationBar | 官方方案，四 Tab 底栏 |
| 网络 | OkHttp（SSE 用原生 BufferedSource 逐行解析） | 流式标准做法 |
| 本地存储 | Room（会话/消息/知识条目）+ DataStore（设置/主题偏好）+ EncryptedSharedPreferences/Keystore（API Key） | |
| 序列化 | kotlinx.serialization（API 报文 + 知识整理 JSON 输出解析） | |
| 依赖注入 | Hilt 可选（也可手写简单注入） | |

包名约定：`com.recaitelab.app`（applicationId 全小写；应用显示名 recAIteLAB）。
包结构：`domain/model`、`data/remote`、`data/repository`、`data/local`、`ui/chat`、`ui/history`、`ui/knowledge`、`ui/settings`、`ui/theme`。

## 四、关键架构决策：Provider 抽象层

主流厂商（OpenAI、DeepSeek、Moonshot、智谱、通义、OpenRouter、SiliconFlow 等）均兼容 OpenAI `POST /v1/chat/completions` 格式：

```kotlin
interface ChatProvider {
    suspend fun streamChat(messages: List<ChatMessage>, model: String, temperature: Float): Flow<String>
    suspend fun fetchModels(): List<String>  // GET {baseUrl}/models
}
```

- 默认实现 `OpenAiCompatibleProvider` 覆盖 80% 厂商；Gemini/Claude 各写适配实现
- 换模型 = 换 `baseUrl + apiKey + model`；模型列表动态拉取不硬编码
- **知识库整理复用同一接口**：`streamChat(...).fold("") { acc, s -> acc + s }` 收集完整文本后解析 JSON，无需新网络代码

## 五、数据模型（Room 五张表）

- **Provider**：id、名称、baseUrl、apiKey（加密）、类型（openai_compatible/gemini/anthropic）
- **Model**：id、providerId、模型名、能力标记（多模态/推理）
- **Conversation**：id、标题、当前 providerId + model、创建/更新时间
- **Message**：id、conversationId、role（user/assistant/system）、content、token 用量、时间戳
- **KnowledgeEntry（新增）**：id、title、summary、content（Markdown）、tags、sourceConversationId（来源会话，可空）、整理所用 providerId/model、createdAt、updatedAt

## 六、知识库功能详细设计（新增）

### 流程

```
历史记录页选会话 →「整理为知识」→ 拼接该会话消息文本
→ 当前 Provider 模型 + 整理 Prompt（非流式收集完整输出）
→ 解析 JSON {title, summary, content, tags}
→ 存入 KnowledgeEntry 表 → 知识库 Tab 展示
```

### 整理 Prompt 模板

```text
你是知识整理助手。请阅读以下对话记录，提炼其中有长期价值的知识，
只输出 JSON，不要输出其他内容：
{"title":"知识标题","summary":"一句话摘要","content":"Markdown 正文","tags":["标签1","标签2"]}
要求：去除寒暄与重复；保留结论、方法、代码片段、踩坑经验；
正文用 Markdown 分节整理；tags 不超过 5 个。
对话记录如下：
【消息列表按 role: content 拼接】
```

### 技术要点与兜底

1. 模型输出可能带 ```json 围栏或多余文字：解析前先提取首个 `{` 到末个 `}` 的子串再反序列化
2. 解析失败兜底：title="未命名整理"、content=原始输出、summary 取前 100 字，**绝不因解析失败丢失内容**
3. 整理中显示进度（Loading），期间禁止重复触发；失败可重试
4. 超长对话沿用上下文截断策略（保留 system + 最近 N 条）；分段整理留二期
5. MVP 仅手动触发；知识条目与来源会话关联，可从详情跳回历史会话

## 七、Vibe Coding 工具链

| 工具 | 角色 | 成本 |
|---|---|---|
| DeepSeek harness（本机终端 Agent） | 主力编码：环境搭建 + 逐功能实现 | 按量 |
| Android Studio | 编译/调试兜底（可选 IDE） | 免费 |
| Git + GitHub | 每功能点提交，随时回滚（必须） | 免费 |
| 真机 adb（已弃用模拟器） | 验证设备；写代码/编译时不需连接，installDebug 与验收时连接 | — |

## 八、开发流程（七阶段）

### 阶段 0：环境与规范（0.5 天）
E0-0~E0-6（真机 adb 版）→ P0-R 改名重构（recAIteLAB / com.recaitelab.app）→ P0-1 CLAUDE.md → P0-2 PRD.md（含知识库）

### 阶段 1：最小可用对话（1–2 天）
P1-1~P1-6：数据模型 → SSE 流式客户端 → Repository → ViewModel → Material 3 聊天页+主题 → Key 配置
✅ 真机验收：逐字流式输出、停止中断、多轮上下文正确、深浅色跟随系统

### 阶段 2：多 Provider 与模型切换（1–2 天）
Provider 抽象层落地、Provider 管理 UI、模型动态拉取、对话页模型选择器
✅ 真机验收：两个厂商 Key 来回切换对话正常

### 阶段 3：本地持久化 + 四 Tab 底栏导航（1–1.5 天）
Room 建表（含 KnowledgeEntry 空表）；历史记录 Tab（会话列表）；四 Tab NavigationBar 骨架（知识库/设置占位）；Key 加密存储
✅ 真机验收：底栏四 Tab 切换正常；`adb shell am force-stop` 杀进程重进历史与配置都在

### 阶段 4：知识库（1.5–2 天）★ 新增
历史记录页「整理为知识」入口 → 整理 Prompt + 非流式调用 → JSON 解析与兜底 → KnowledgeEntry 落库 → 知识库 Tab（列表/详情/搜索/删除）→ 详情 Markdown 渲染
✅ 真机验收：选一个会话整理后知识库出现条目；详情可查看；杀进程后仍在；解析失败有兜底不丢内容

### 阶段 5：体验打磨（1–2 天）
Markdown 渲染（对话页）、代码块复制、错误提示（Key 失效/断网/限流）、上下文截断策略；主题美化（多主题切换、Material You 动态取色）；App 图标（自适应图标 + monochrome）
✅ 真机验收：断网/错 Key/长对话异常友好；深浅色与主题切换无硬编码颜色；图标各遮罩不裁切

### 阶段 6：测试与发布（1 天）
单元测试（SSE 解析、截断、知识 JSON 解析与兜底）；assembleRelease 签名包真机冒烟；上架合规
✅ 真机验收：release 包正常安装运行

### Vibe Coding 实操要点
1. 一次一个小功能 → 立即 `assembleDebug` 编译 → 通过即 Git 提交
2. 报错贴完整堆栈给 AI 修，不自己猜；连续 3 次修不好回滚换角度
3. API Key 代码人工过目；真实 Key 只进 local.properties，不发给 Agent
4. 写代码/编译不需要手机；仅 installDebug 与真机验收时连接
5. UI 一律 Material 3：颜色取 colorScheme，禁止硬编码颜色字号

## 九、安全与架构提醒

- **纯本地 BYO-Key**（Key 只存本机、直连官方 API）：最简单隐私最好，适合个人工具；知识库数据也全在本地 Room
- **分发型产品**：Key 不能打进 APK，需加轻量后端转发 + 鉴权 + 计量，架构已预留
- 知识整理会把对话内容发给当前 Provider 的模型——属用户主动触发，隐私边界与聊天一致

## 十、成本与周期估算

- **时间**：业余投入 8–12 天出含知识库的 MVP（原 5–8 天 + 知识库约 2 天 + 导航约 0.5 天）
- **工具**：Agent 按量/订阅
- **API 调试费**：DeepSeek 按量计费，调试 + 知识整理成本低（几元量级）

# recAIteLAB

一款本地优先的 Android 聚合 AI 聊天与知识库应用。

recAIteLAB 支持通过自带的 API Key（BYO-Key）接入 OpenAI 兼容的第三方模型服务，在一个应用内完成多模型对话、历史会话管理，并将有价值的对话重新整理为个人知识库。

## ✨ 主要功能

### AI 对话
- 多轮对话与 SSE 流式输出
- 停止生成、失败重试
- Markdown 渲染与代码块复制
- 上下文超长截断，本地完整历史不受影响

### 多 Provider 与模型切换
- 管理多个 OpenAI 兼容 Provider：名称、Base URL、API Key
- 通过 `GET /models` 动态拉取模型列表，也支持手动添加
- 在对话页顶部随时切换 Provider 和模型
- 默认不发送 `temperature`，由模型服务端使用各自默认值，兼容对温度参数有限制的模型

### 历史记录
- 新建、删除、重命名会话
- 会话与消息通过 Room 本地持久化
- 不同会话的上下文严格隔离
- 杀进程后恢复历史消息、Provider 配置和选中模型

### 知识库
- 从历史会话一键发起「整理为知识」
- 使用当前模型提炼标题、摘要、Markdown 正文和标签
- 知识条目列表、搜索、详情查看和删除
- 支持从知识详情返回来源会话
- 模型输出不是合法 JSON 时自动保存原始内容，避免整理结果丢失

### 界面与个性化
- 四个底栏页面：AI 对话、历史记录、知识库、设置
- Jetpack Compose + Material Design 3
- 跟随系统、浅色、深色三种外观模式
- 多套配色方案，支持 Android 12+ 动态取色
- 自适应启动图标，支持 Android 13+ 单色主题图标

## 📱 页面结构

```text
AI 对话       历史记录       知识库        设置
   │              │             │            │
   ├─ 消息列表     ├─ 会话列表    ├─ 知识列表   ├─ Provider 管理
   ├─ 模型切换     ├─ 新建/删除   ├─ 搜索/删除  ├─ 主题设置
   └─ 流式对话     └─ 整理为知识  └─ 知识详情   └─ API 参数配置
```

## 🛠 技术栈

| 分类 | 技术 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose、Material 3 |
| 架构 | MVVM、ViewModel、StateFlow |
| 导航 | Navigation Compose、NavigationBar |
| 网络 | OkHttp、SSE 流式响应 |
| 序列化 | kotlinx.serialization |
| 本地数据 | Room、DataStore |
| 密钥保护 | Android Keystore / EncryptedSharedPreferences |
| 测试 | JUnit、本地 JVM 单元测试 |

应用包名：`com.recaitelab.app`  
最低系统版本：Android 8.0（API 26，以项目 Gradle 配置为准）

## 🚀 构建与运行

### 环境要求

- JDK 17+
- Android Studio，或 Android SDK Command-line Tools
- 项目 Gradle 配置所要求的 Android SDK Platform 与 Build Tools
- 一台开启 USB 调试的 Android 真机，或可用的 Android 模拟器

### 1. 获取项目

```bash
git clone https://github.com/super-star12345/recAIteLAB
cd recAIteLAB
```

### 2. 配置本地属性

在项目根目录的 `local.properties` 中配置 SDK 路径：

```properties
sdk.dir=/path/to/Android/sdk
```

如需在首次启动时初始化默认 DeepSeek Provider，可以额外填写：

```properties
DEEPSEEK_API_KEY=你的APIKey
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
```

也可以不在这里填写 Key，启动后直接在「设置」页面添加 Provider。

### 3. 编译

```bash
./gradlew assembleDebug
```

### 4. 安装到真机

```bash
adb devices
./gradlew installDebug
adb shell am start -n com.recaitelab.app/.MainActivity
```

编写代码和执行 `assembleDebug` 时不需要连接手机；只有安装、真机验收和查看设备日志时需要连接。

## 📖 使用指南

1. 打开「设置」，新增 Provider。
2. 填写名称、Base URL 和 API Key，当前接口类型为 OpenAI 兼容格式。
3. 点击「拉取模型列表」，或手动添加模型名称。
4. 回到「AI 对话」，在顶部选择要使用的 Provider 和模型。
5. 在「历史记录」中管理会话，点击「整理为知识」将对话沉淀到知识库。
6. 在「知识库」中搜索、查看或删除整理后的知识条目。

已验证的典型服务包括 DeepSeek、Moonshot Kimi；其他服务只要兼容 OpenAI 的 `/chat/completions` 和 `/models` 接口，也可以尝试接入。

## 🧪 测试

运行本地单元测试：

```bash
./gradlew testDebugUnitTest
```

测试重点包括：

- SSE 多行事件、空行分隔和 `[DONE]` 终止处理
- 上下文截断与会话隔离
- 知识整理 JSON 的围栏、杂文本和解析失败兜底
- 模型列表响应解析

## 📦 Release 构建

release 签名信息通过项目根目录的 `keystore.properties` 提供：

```properties
storeFile=/path/to/your-keystore.jks
storePassword=你的密钥库密码
keyAlias=recaitelab
keyPassword=你的密钥密码
```

构建命令：

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

APK 输出位置：

```text
app/build/outputs/apk/release/app-release.apk
```

> 请妥善备份签名密钥库和密码。`keystore.properties`、`*.jks`、`*.keystore` 均不应提交到 Git。
> Debug 与 Release 签名不同；卸载已有调试包会清空本地聊天记录和知识库，安装前请确认。

## 🗂 项目结构

```text
com.recaitelab.app
├── domain/model          # 数据模型与领域对象
├── data
│   ├── remote            # Provider 抽象、API 与 SSE 客户端
│   ├── repository        # 对话、Provider、知识库等仓库
│   └── local             # Room、DataStore 与密钥存储
└── ui
    ├── chat              # AI 对话
    ├── history           # 历史记录
    ├── knowledge         # 知识库列表与详情
    ├── settings          # 设置
    ├── common            # Markdown 等可复用组件
    └── theme             # Material 3 主题
```

## 🔐 隐私与安全

- API Key 仅保存在本机，并采用加密存储。
- 会话、消息和知识条目默认保存在本地 Room 数据库。
- 应用不依赖开发者自有的中转服务器。
- 对话和知识整理会将相应内容发送给你主动选择的第三方模型服务，其数据处理规则以对应服务商的隐私政策为准。
- 日志不应输出 API Key；请勿提交 `local.properties`、签名配置或密钥文件。

## 🗺 后续规划

- 知识库问答（RAG-lite）
- 知识条目编辑与 Markdown 导出
- 批量或自动整理历史会话
- 超长对话分段整理
- 可配置的整理专用模型
- Gemini、Anthropic 原生接口适配
- 思考链展示、联网搜索、多模态输入和 Token 用量统计

## 🤝 开发方式

本项目采用 AI Agent 辅助的分阶段开发流程：环境搭建 → 最小对话 → 多 Provider → 持久化与导航 → 知识库 → 体验打磨 → 测试发布。

每个功能点独立实现、编译验证并提交 Git，通过项目规则文档约束技术栈、包结构、Material 3 样式和密钥安全。

## License

本项目基于 [GNU General Public License v3.0 或更高版本](LICENSE) 开源。

SPDX 许可证标识：`GPL-3.0-or-later`。

你可以自由使用、修改和分发本项目；分发修改版或衍生作品时，需要提供对应源代码，并继续以 GPLv3 或更高版本授权。

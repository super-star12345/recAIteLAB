package com.recaitelab.app.domain.model

/**
 * 面向用户的错误分类，对话与知识整理共用。
 *
 * 具体文案由 UI 层按 `strings.xml` 解析，ViewModel 不持有资源 id 或原始异常。
 */
enum class UserError {
    /** HTTP 401 / 403：Key 无效或无权限。 */
    INVALID_API_KEY,

    /** HTTP 429：频率超限或额度不足。 */
    RATE_LIMITED,

    /** UnknownHost / SocketTimeout / 断网等 IO 异常。 */
    NETWORK,

    /** 没有可用的 Provider / 模型等前置条件不满足。 */
    CONFIGURATION,

    /** 会话不存在或已被删除。 */
    NOT_FOUND,

    /** 其余异常：只展示简短通用提示，原始信息写日志。 */
    UNKNOWN,
}

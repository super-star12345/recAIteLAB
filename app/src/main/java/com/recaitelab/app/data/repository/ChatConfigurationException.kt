package com.recaitelab.app.data.repository

/**
 * 会话因配置不完整而无法发起时的错误。
 *
 * [message] 是可直接展示给用户的中文提示。
 */
sealed class ChatConfigurationException(message: String) : IllegalStateException(message) {

    /** 当前没有任何激活的 Provider。 */
    class NoActiveProvider : ChatConfigurationException("尚未配置 Provider，请先添加并激活一个")

    /** 激活的 Provider 还没有选择模型。 */
    class NoSelectedModel(providerName: String) :
        ChatConfigurationException("「$providerName」尚未选择模型，请先选择模型")
}

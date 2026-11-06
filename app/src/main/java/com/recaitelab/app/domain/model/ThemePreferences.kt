package com.recaitelab.app.domain.model

/** 外观模式：跟随系统 / 强制浅色 / 强制深色。 */
enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** 配色方案：默认青绿 / 护眼绿 / 商务蓝；动态取色开启时会被系统配色覆盖。 */
enum class AppColorTheme {
    DEFAULT,
    GREEN,
    BLUE,
}

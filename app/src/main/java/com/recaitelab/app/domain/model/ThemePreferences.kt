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

/** 设计系统：保留的 Material 3 与新增的 Prism Glass；默认 Material 3。 */
enum class AppDesignSystem {
    MATERIAL_3,
    PRISM_GLASS,
}

/** Prism 配色风格：由种子色派生，区别在强调色强度与表面着色程度。 */
enum class PrismColorStyle {
    /** 强调种子色，背景相对中性。 */
    VIVID,

    /** 页面与表面带种子色色相。 */
    TINTED,

    /** 低饱和、柔和。 */
    SOFT,

    /** 灰阶为主，保留错误色。 */
    MONOCHROME,
}

/** 玻璃质量档位；T1 只持久化设置，不实际启用玻璃效果。 */
enum class GlassQuality {
    OFF,
    STANDARD,
    ULTRA,
}

/** Prism 默认种子色（品牌青绿）。 */
const val DEFAULT_PRISM_SEED_ARGB: Long = 0xFF006A6A

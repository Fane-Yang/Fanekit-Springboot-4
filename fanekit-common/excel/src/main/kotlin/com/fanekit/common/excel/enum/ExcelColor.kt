package com.fanekit.common.excel.enum

import org.apache.poi.ss.usermodel.IndexedColors

/**
 * Excel颜色枚举
 * 提供常用的Excel单元格颜色，避免外部模块直接依赖Apache POI
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/23
 * @since 1.0.0
 * @version 1.0.0
 */
enum class ExcelColor(val indexedColor: IndexedColors) {
    /** 自动颜色 */
    AUTOMATIC(IndexedColors.AUTOMATIC),
    /** 黑色 */
    BLACK(IndexedColors.BLACK),
    /** 白色 */
    WHITE(IndexedColors.WHITE),
    /** 红色 */
    RED(IndexedColors.RED),
    /** 绿色 */
    GREEN(IndexedColors.GREEN),
    /** 蓝色 */
    BLUE(IndexedColors.BLUE),
    /** 黄色 */
    YELLOW(IndexedColors.YELLOW),
    /** 粉色 */
    PINK(IndexedColors.PINK),
    /** 青色 */
    TURQUOISE(IndexedColors.TURQUOISE),
    /** 深红色 */
    DARK_RED(IndexedColors.DARK_RED),
    /** 深绿色 */
    DARK_GREEN(IndexedColors.DARK_GREEN),
    /** 深蓝色 */
    DARK_BLUE(IndexedColors.DARK_BLUE),
    /** 橙色 */
    ORANGE(IndexedColors.ORANGE),
    /** 浅蓝色 */
    LIGHT_BLUE(IndexedColors.LIGHT_BLUE),
    /** 浅绿色 */
    LIGHT_GREEN(IndexedColors.LIGHT_GREEN),
    /** 浅黄色 */
    LIGHT_YELLOW(IndexedColors.LIGHT_YELLOW),
    /** 浅橙色 */
    LIGHT_ORANGE(IndexedColors.LIGHT_ORANGE),
    /** 灰色25% */
    GREY_25_PERCENT(IndexedColors.GREY_25_PERCENT),
    /** 灰色40% */
    GREY_40_PERCENT(IndexedColors.GREY_40_PERCENT),
    /** 灰色50% */
    GREY_50_PERCENT(IndexedColors.GREY_50_PERCENT),
    /** 灰色80% */
    GREY_80_PERCENT(IndexedColors.GREY_80_PERCENT);

    companion object {
        /**
         * 从IndexedColors转换为ExcelColor
         */
        fun fromIndexedColors(indexedColors: IndexedColors): ExcelColor {
            return entries.find { it.indexedColor == indexedColors } ?: AUTOMATIC
        }
    }
}

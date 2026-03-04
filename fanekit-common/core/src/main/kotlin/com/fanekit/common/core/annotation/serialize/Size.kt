package com.fanekit.common.core.annotation.serialize

// 校验字符串、集合、数组的大小范围
/**
 * Size注解 - 用于校验字符串、集合或数组的大小范围
 * @property min 最小大小，默认为0
 * @property max 最大大小，默认为Int.MAX_VALUE
 * @property message 错误消息，默认为"Size out of range"
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Size(
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE,
    val message: String = "Size out of range"
)
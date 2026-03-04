package com.fanekit.common.core.annotation.serialize

// 校验日期是否在当前时间之后，时间必须符合在当前日期之后
/**
 * 校验日期是否在当前时间之后
 * @property message 错误提示信息
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Future(val message: String = "Date must be in the future")
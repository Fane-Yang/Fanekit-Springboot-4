package com.fanekit.common.core.annotation.serialize

/**
 * 校验字符串是否匹配正则表达式
 * 只有字符串长度>=1才会校验
 * 邮箱: regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
 * 单个字符："^(Y|N)$"
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Pattern(
    val regex: String,
    val message: String = "Value does not match the required pattern"
)
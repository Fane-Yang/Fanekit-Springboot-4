package com.fanekit.common.core.annotation.serialize

/**
 * 序列化注解
 * 用于限制属性值的最大值
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Max(val value: Int, val message: String = "Value is greater than the maximum")
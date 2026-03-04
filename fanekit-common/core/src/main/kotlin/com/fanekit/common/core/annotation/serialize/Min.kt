package com.fanekit.common.core.annotation.serialize


/**
 * 序列化注解
 * 用于标记属性或字段的最小值验证
 * @property value 最小值
 * @property message 验证失败时的错误消息
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Min(val value: Int, val message: String = "Value is less than the minimum")

package com.fanekit.common.core.annotation.serialize
// 检查字段是否为空或null，适用于字符串、集合、数组等 也适用于非null检测
/**
 * 注解：检查字段是否为空或null
 * 适用于字符串、集合、数组等
 * 也适用于非null检测
 * @property message 错误消息，默认值为 "Field cannot be empty"
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotEmpty(val message: String = "Field cannot be empty")
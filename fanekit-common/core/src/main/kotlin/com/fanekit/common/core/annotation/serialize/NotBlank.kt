package com.fanekit.common.core.annotation.serialize
// 检查字段是否为空白字符
/**
 * 注解：检查字段是否为空白字符
 * 适用于属性、字段和函数
 * @property message 错误消息，默认值为 "Field cannot be blank"
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotBlank(val message: String = "Field cannot be blank")
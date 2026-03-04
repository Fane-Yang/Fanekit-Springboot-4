package com.fanekit.common.core.annotation.serialize
// 检查字段是否为null，支持任何类型，支持对类进行校验
/**
 * 注解：字段不能为空
 * 用于标记类的属性或字段，确保在序列化或反序列化时该字段不能为空。
 * 可以用于数据模型、DTO等场景，确保数据完整性。
 * @property message 错误消息，默认值为 "Field cannot be null"
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotNull(val message: String = "Field cannot be null")
package com.fanekit.common.core.annotation.serialize
// 校验日期是否在当前日期之前，时间必须在当前日期前
/**
 * 校验日期是否在当前日期之前，时间必须在当前日期前
 * 适用于需要验证日期字段的场景，例如在表单提交时确保用户输入的日期是过去的日期。
 * 可以用于数据模型的属性上，确保在序列化或反序列化时进行验证。
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/23
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.PROPERTY,AnnotationTarget.FIELD,AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Past(val message: String = "Date must be in the past")
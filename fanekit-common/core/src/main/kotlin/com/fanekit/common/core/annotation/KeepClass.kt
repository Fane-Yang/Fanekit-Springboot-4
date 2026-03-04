package com.fanekit.common.core.annotation

/**
 * 注解类，用于标记需要保留的类，代码混淆时会保留类名和方法名
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/07/31
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeepClass
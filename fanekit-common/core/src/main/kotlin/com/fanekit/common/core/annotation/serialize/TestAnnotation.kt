package com.fanekit.common.core.annotation.serialize

import java.util.*

/**
 * 测试注解类，包含各种验证注解
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/24
 * @since 1.0.0
 * @version 1.0.0
 */
data class TestAnnotation(
    @NotNull(message = "名字 不可为空")
    val name: String?,

    @NotEmpty
    val tags: List<String>?,

    @Size(min = 5, max = 100, message = "Description length must be between 5 and 100")
    val description: String?,

    @Pattern(regex = "^[a-zA-Z0-9_]+$", message = "Username can only contain alphanumeric characters and underscores")
    val username: String?,

    @Future(message = "Start date must be in the future")
    val startDate: Date?,

    @Past(message = "Birth date must be in the past")
    val birthDate: java.time.LocalDateTime?
)
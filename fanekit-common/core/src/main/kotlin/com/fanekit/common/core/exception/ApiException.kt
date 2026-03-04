package com.fanekit.common.core.exception

import com.fanekit.common.core.constants.ErrorCodes


/**
 * 统一业务异常：
 * @param code 对应 [ErrorCodes] 常量
 * @param message 若为空→自动从 [ErrorCodes.defaultMessages] 获取
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/04/23
 * @since 1.0.0
 * @version 1.0.0
 */
class ApiException(
    val code: Int,
    override val message: String =
        ErrorCodes.defaultMessages[code] ?: "Error"
) : RuntimeException(message)
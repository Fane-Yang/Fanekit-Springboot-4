package com.fanekit.common.core.exception

import com.fanekit.common.core.constants.ErrorCodes

/**
 * 工具类异常：
 * @param code 对应 [ErrorCodes] 常量中的工具类错误码
 * @param message 若为空→自动从 [ErrorCodes.defaultMessages] 获取
 */
class UtilException(
    val code: Int,
    override val message: String =
        ErrorCodes.defaultMessages[code] ?: "Util Error"
) : RuntimeException(message)

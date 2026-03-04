package com.fanekit.common.core.exception

import com.fanekit.common.core.constants.ErrorCodes
import com.fanekit.common.core.exception.UtilException

/**
 * 快速抛异常工具：
 * 需要自定义抛出异常时，可以使用此工具类。
 *
 * ```kotlin
 * Throw.unauthorized()                  // 401
 * Throw.tokenExpired("登录状态已失效")   // 1003 + 自定义 message
 * Throw.error(ErrorCodes.DATA_NOT_FOUND) // 1004
 * Throw.utilIoError("读取失败", e)       // 3000 + 自定义 message + 原始异常
 * ```
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/24
 * @since 1.0.0
 * @version 1.0.0
 */
object Throw {

    /* ───────── HTTP ───────── */

    fun badRequest(msg: String? = null, e: Throwable? = null): Nothing        =
        error(ErrorCodes.BAD_REQUEST, msg, e)

    fun unauthorized(msg: String? = null, e: Throwable? = null): Nothing      =
        error(ErrorCodes.UNAUTHORIZED, msg, e)

    fun forbidden(msg: String? = null, e: Throwable? = null): Nothing         =
        error(ErrorCodes.FORBIDDEN, msg, e)

    fun notFound(msg: String? = null, e: Throwable? = null): Nothing          =
        error(ErrorCodes.PAGE_NOT_FOUND, msg, e)

    fun conflict(msg: String? = null, e: Throwable? = null): Nothing          =
        error(ErrorCodes.CONFLICT, msg, e)

    fun methodNotAllowed(msg: String? = null, e: Throwable? = null): Nothing  =
        error(ErrorCodes.METHOD_NOT_ALLOWED, msg, e)

    fun internalServer(msg: String? = null, e: Throwable? = null): Nothing    =
        error(ErrorCodes.INTERNAL_SERVER_ERROR, msg, e)

    fun serviceUnavailable(msg: String? = null, e: Throwable? = null): Nothing=
        error(ErrorCodes.SERVICE_UNAVAILABLE, msg, e)

    /* ───────── 业务 ───────── */

    fun systemError(msg: String? = null, e: Throwable? = null): Nothing       =
        error(ErrorCodes.SYSTEM_ERROR, msg, e)

    fun tokenInvalid(msg: String? = null, e: Throwable? = null): Nothing      =
        error(ErrorCodes.TOKEN_INVALID, msg, e)

    fun tokenMissing(msg: String? = null, e: Throwable? = null): Nothing      =
        error(ErrorCodes.TOKEN_MISSING, msg, e)

    fun tokenExpired(msg: String? = null, e: Throwable? = null): Nothing      =
        error(ErrorCodes.TOKEN_EXPIRED, msg, e)

    fun dataNotFound(msg: String? = null, e: Throwable? = null): Nothing      =
        error(ErrorCodes.DATA_NOT_FOUND, msg, e)

    fun paramInvalid(msg: String? = null, e: Throwable? = null): Nothing      =
        error(ErrorCodes.PARAM_INVALID, msg, e)

    fun permissionDenied(msg: String? = null, e: Throwable? = null): Nothing  =
        error(ErrorCodes.PERMISSION_DENIED, msg, e)

    /* ───────── 流量 / 安全 ───────── */

    fun requestBusy(msg: String? = null, e: Throwable? = null): Nothing       =
        error(ErrorCodes.REQUEST_BUSY, msg, e)

    fun ipBlocked(msg: String? = null, e: Throwable? = null): Nothing         =
        error(ErrorCodes.IP_BLOCKED, msg, e)

    fun rateLimitExceeded(msg: String? = null, e: Throwable? = null): Nothing =
        error(ErrorCodes.RATE_LIMIT_EXCEEDED, msg, e)

    /* ───────── 工具类 ───────── */

    fun utilIoError(msg: String? = null, e: Throwable? = null): Nothing =
        utilError(ErrorCodes.UTIL_IO_ERROR, msg, e)

    fun utilUnsafeOperation(msg: String? = null, e: Throwable? = null): Nothing =
        utilError(ErrorCodes.UTIL_UNSAFE_OPERATION, msg, e)

    fun utilFileNotFound(msg: String? = null, e: Throwable? = null): Nothing =
        utilError(ErrorCodes.UTIL_FILE_NOT_FOUND, msg, e)

    fun utilPermissionDenied(msg: String? = null, e: Throwable? = null): Nothing =
        utilError(ErrorCodes.UTIL_PERMISSION_DENIED, msg, e)

    fun utilInvalidParam(msg: String? = null, e: Throwable? = null): Nothing =
        utilError(ErrorCodes.UTIL_INVALID_PARAM, msg, e)

    fun utilOperationFailed(msg: String? = null, e: Throwable? = null): Nothing =
        utilError(ErrorCodes.UTIL_OPERATION_FAILED, msg, e)

    /* ───────── 通用 ───────── */

    /** 按任意 code 抛错，message 为空则用默认文本，可传入原始异常 */
    fun error(code: Int, msg: String? = null, e: Throwable? = null): Nothing =
        throw ApiException(code, msg ?: ErrorCodes.defaultMessages[code] ?: "Error").apply {
            e?.let { initCause(it) }
        }

    /** 按任意 code 抛工具类异常，message 为空则用默认文本，可传入原始异常 */
    fun utilError(code: Int, msg: String? = null, e: Throwable? = null): Nothing =
        throw UtilException(code, msg ?: ErrorCodes.defaultMessages[code] ?: "Util Error").apply {
            e?.let { initCause(it) }
        }

    /* ───────── 条件式校验示例 ───────── */

    inline fun requireTrue(
        condition: Boolean,
        code: Int = ErrorCodes.PARAM_INVALID,
        lazyMsg: () -> String = { ErrorCodes.defaultMessages[code] ?: "Invalid" },
        e: Throwable? = null
    ) {
        if (!condition) error(code, lazyMsg(), e)
    }
}
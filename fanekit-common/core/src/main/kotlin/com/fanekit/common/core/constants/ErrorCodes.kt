package com.fanekit.common.core.constants

/**
 * 错误码常量类
 * 定义了通用的错误码和业务自定义的错误码
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/04/23
 * @since 1.0.0
 * @version 1.0.0
 */
object ErrorCodes {
    /* ───── HTTP 通用 ───── */
    const val SUCCESS                = 200      // OK
    const val BAD_REQUEST            = 400      // 参数错误
    const val UNAUTHORIZED           = 401      // 未认证
    const val FORBIDDEN              = 403      // 权限不足
    const val PAGE_NOT_FOUND         = 404      // 资源不存在
    const val CONFLICT               = 409      // 资源冲突
    const val METHOD_NOT_ALLOWED     = 415      // 请求方法不允许
    const val INTERNAL_SERVER_ERROR  = 500      // 服务器错误
    const val SERVICE_UNAVAILABLE    = 503      // 服务不可用

    /* ───── 业务自定义 ───── */
    const val SYSTEM_ERROR           = 1000     // 系统异常
    const val TOKEN_INVALID          = 1001     // TOKEN/JWT认证不通过（验证码 / 签名…）
    const val TOKEN_MISSING          = 1002     // TOKEN/JWT缺失
    const val TOKEN_EXPIRED          = 1003     // TOKEN/JWT过期 登录过期
    const val DATA_NOT_FOUND         = 1004     // 数据不存在
    const val PARAM_INVALID          = 1005     // 参数非法
    const val PERMISSION_DENIED      = 1006     // 操作被拒绝

    /* ───── 流量 / 安全 ───── */
    const val REQUEST_BUSY           = 2000     // 服务繁忙
    const val IP_BLOCKED             = 2001     // IP 被封禁
    const val RATE_LIMIT_EXCEEDED    = 2002     // 频率超限

    /* ───── 工具类 ───── */
    const val UTIL_IO_ERROR          = 3000     // IO操作错误（通用）
    const val UTIL_UNSAFE_OPERATION  = 3001     // 不安全操作，例如文件路径../../
    const val UTIL_FILE_NOT_FOUND    = 3002     // 文件/目录不存在
    const val UTIL_PERMISSION_DENIED = 3003     // 权限不足
    const val UTIL_INVALID_PARAM     = 3004     // 参数无效
    const val UTIL_OPERATION_FAILED  = 3005     // 操作失败

    /** code → 默认 message */
    val defaultMessages = mapOf(
        /* HTTP */
        SUCCESS               to "Success",
        BAD_REQUEST           to "Bad request",
        UNAUTHORIZED          to "Unauthorized",
        FORBIDDEN             to "Forbidden",
        PAGE_NOT_FOUND        to "Not found",
        CONFLICT              to "Conflict",
        INTERNAL_SERVER_ERROR to "Internal server error",
        METHOD_NOT_ALLOWED    to "Method Not Allowed",
        SERVICE_UNAVAILABLE   to "Service unavailable",
        /* Business */
        SYSTEM_ERROR          to "System error",
        TOKEN_INVALID         to "Token invalid",
        TOKEN_MISSING         to "Token missing",
        TOKEN_EXPIRED         to "Token expired",
        DATA_NOT_FOUND        to "Data not found",
        PARAM_INVALID         to "Invalid parameters",
        PERMISSION_DENIED     to "Permission denied",
        /* Traffic / Security */
        REQUEST_BUSY          to "Server busy, try later",
        IP_BLOCKED            to "IP blocked",
        RATE_LIMIT_EXCEEDED   to "Rate limit exceeded",
        /* Util Operations */
        UTIL_IO_ERROR         to "IO operation error",
        UTIL_UNSAFE_OPERATION to "Unsafe operation detected",
        UTIL_FILE_NOT_FOUND   to "File or directory not found",
        UTIL_PERMISSION_DENIED to "Permission denied",
        UTIL_INVALID_PARAM    to "Invalid parameters",
        UTIL_OPERATION_FAILED to "Operation failed"
    )



    /* ── code → HttpStatus ──────────────────────────────────────────────────────── */

    fun toHttpStatusCode(code: Int): Int = when (code) {
        /* HTTP 映射 */
        // 将后面的代码改为HTTP状态码
        BAD_REQUEST,
        PARAM_INVALID                 -> 400 // 400 Bad Request

        UNAUTHORIZED,
        TOKEN_INVALID,
        TOKEN_MISSING,
        TOKEN_EXPIRED                 -> 401 // 401 Unauthorized

        FORBIDDEN,
        PERMISSION_DENIED,
        IP_BLOCKED                    -> 403 // 403 Forbidden

        PAGE_NOT_FOUND,
        DATA_NOT_FOUND                -> 404 // 404 Not Found

        CONFLICT                      -> 409 // 409 Conflict
        METHOD_NOT_ALLOWED            -> 415 // 415 Method Not Allowed

        REQUEST_BUSY,
        RATE_LIMIT_EXCEEDED           -> 429 // 429 Too Many Requests

        SERVICE_UNAVAILABLE           -> 503 // 503 Service Unavailable
        else                          -> 500 // 500 Internal Server Error
    }
}
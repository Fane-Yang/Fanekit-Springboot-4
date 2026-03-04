package com.fanekit.common.core.api

import com.fanekit.common.core.constants.ErrorCodes

/**
 * 通用的 API 返回数据类
 * 用于统一处理 API 响应格式
 * 包含状态码、成功标志、消息、数据和总数等字段
 * 提供了成功和失败的静态方法用于创建响应对象
 * @param code 状态码，默认为 200
 * @param success 操作是否成功
 * @param message 描述信息，默认为 null
 * @param total 总数，默认为 null
 * @param data 响应数据，默认为 null
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/4/24
 * @since 1.0.0
 * @version 1.0.0
 */
data class ApiResponse(
    val code: Int = ErrorCodes.SUCCESS,        // 状态码
    val success: Boolean, // 操作成功/失败标记
    val message: String? = null,  // 描述信息
    var total: Long? = null,
    val data: Any? = null
) {
    init {
        // 如果 data 是 List、ArrayList、Set 或 HashSet，自动设置 total 为集合的大小，无需用户收到设置
        total = when (data) {
            is List<*> -> data.size.toLong()  // 如果是 List 或 ArrayList
//            is Set<*> -> data.size.toLong()   // 如果是 Set 或 HashSet
//            is Map<*, *> -> data.size.toLong() // 如果是 Map 或 HashMap
            else -> total            // 否则保持原始值
        }
    }

    companion object {
        fun success(
            message: String? = null,
            data: Any? = null,
            total: Long? = null
        ): ApiResponse {
            return ApiResponse(
                code = ErrorCodes.SUCCESS,
                success = true,
                message = message ?: ErrorCodes.defaultMessages[ErrorCodes.SUCCESS],
                data = data,
                total = total
            )
        }
        fun failure(
            code: Int = ErrorCodes.SYSTEM_ERROR,
            message: String = "请求失败",
            data: Any? = null
        ): ApiResponse {
            return ApiResponse(
                code = code,
                success = false,
                message = message,
                data = data
            )
        }

        fun error(code: Int,message: String?): ApiResponse {
            return ApiResponse(
                code = code,
                success = false,
                message = message ?: ErrorCodes.defaultMessages[code],
            )
        }
        fun systemError(message: String?): ApiResponse {
            return ApiResponse(
                code = ErrorCodes.SYSTEM_ERROR,
                success = false,
                message = message ?: ErrorCodes.defaultMessages[ErrorCodes.SYSTEM_ERROR],
            )
        }
        fun notFound(): ApiResponse {
            return ApiResponse(
                code = 404,
                success = false,
                message = ErrorCodes.defaultMessages[404],
            )
        }
    }
}


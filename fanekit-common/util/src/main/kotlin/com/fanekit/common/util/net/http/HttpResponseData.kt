package com.fanekit.common.util.net.http

/**
 * 封装响应结果
 * @param success 操作是否成功
 * @param code HTTP 状态码
 * @param headers 响应头
 * @param bodyString 响应体字符串
 * @param errorMessage 错误信息（仅在失败时有值）
 */
data class HttpResponseData(
    val success: Boolean,
    val code: Int = 0,
    val headers: Map<String, List<String>> = emptyMap(),
    val bodyString: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success(code: Int, headers: Map<String, List<String>>, bodyString: String?) =
            HttpResponseData(true, code, headers, bodyString)

        fun failure(errorMessage: String, code: Int = 0) =
            HttpResponseData(false, code, emptyMap(), null, errorMessage)
    }
}

typealias HttpCallback = (HttpResponseData?, Throwable?) -> Unit

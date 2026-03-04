package com.fanekit.common.util.net.http

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager



/**
 * 使用 OkHttp3 进行 HTTP 请求的工具类。
 * 客户端配置在构造时完成，实例可复用。
 *
 * @param connectTimeoutMillis 连接超时时间（毫秒）
 * @param readTimeoutMillis 读取超时时间（毫秒）
 * @param writeTimeoutMillis 写入超时时间（毫秒）
 * @param unsafe 是否忽略 SSL 证书验证
 */
class HttpClient(
    connectTimeoutMillis: Long = 30_000,
    readTimeoutMillis: Long = 30_000,
    writeTimeoutMillis: Long = 30_000,
    unsafe: Boolean = false
) {

    private val client: OkHttpClient

    init {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeoutMillis, TimeUnit.MILLISECONDS)

        if (unsafe) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                val sslSocketFactory = sslContext.socketFactory

                builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                throw RuntimeException("Failed to create unsafe OkHttpClient", e)
            }
        }
        client = builder.build()
    }

    //region ========== 同步请求 ==========

    fun get(url: String, queries: Map<String, String>? = null, headers: Map<String, String>? = null): HttpResponseData {
        return try {
            val request = buildGetRequest(url, queries, headers)
            executeSync(request)
        } catch (e: Exception) {
            HttpResponseData.failure("GET request failed: ${e.message}")
        }
    }

    fun downloadFile(url: String, saveToFile: File, queries: Map<String, String>? = null, headers: Map<String, String>? = null): HttpResponseData {
        return try {
            val request = buildGetRequest(url, queries, headers)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream().use { input ->
                        saveToFile.outputStream().use { output ->
                            input?.copyTo(output) ?: throw IOException("Response body is null")
                        }
                    }
                    HttpResponseData.success(response.code, response.headers.toMultimap(), "File downloaded successfully")
                } else {
                    HttpResponseData.failure("HTTP ${response.code}: ${response.message}", response.code)
                }
            }
        } catch (e: Exception) {
            HttpResponseData.failure("Download failed: ${e.message}")
        }
    }

    fun postJson(url: String, jsonBody: String, headers: Map<String, String>? = null): HttpResponseData {
        return try {
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = buildPostRequest(url, requestBody, headers)
            executeSync(request)
        } catch (e: Exception) {
            HttpResponseData.failure("POST JSON request failed: ${e.message}")
        }
    }

    fun postForm(url: String, formParams: Map<String, String>, headers: Map<String, String>? = null): HttpResponseData {
        return try {
            val formBody = FormBody.Builder().apply {
                formParams.forEach { (k, v) -> add(k, v) }
            }.build()
            val request = buildPostRequest(url, formBody, headers)
            executeSync(request)
        } catch (e: Exception) {
            HttpResponseData.failure("POST form request failed: ${e.message}")
        }
    }

    fun postMultipart(url: String, formFields: Map<String, String>? = null, fileParams: Map<String, File>? = null, headers: Map<String, String>? = null): HttpResponseData {
        return try {
            val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                formFields?.forEach { (k, v) -> addFormDataPart(k, v) }
                fileParams?.forEach { (fieldName, file) ->
                    val fileBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    addFormDataPart(fieldName, file.name, fileBody)
                }
            }.build()
            val request = buildPostRequest(url, multipartBody, headers)
            executeSync(request)
        } catch (e: Exception) {
            HttpResponseData.failure("POST multipart request failed: ${e.message}")
        }
    }

    fun putJson(url: String, jsonBody: String, headers: Map<String, String>? = null): HttpResponseData {
        return try {
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder().url(url).put(requestBody).apply {
                headers?.forEach { (k, v) -> addHeader(k, v) }
            }.build()
            executeSync(request)
        } catch (e: Exception) {
            HttpResponseData.failure("PUT JSON request failed: ${e.message}")
        }
    }

    fun delete(url: String, queries: Map<String, String>? = null, headers: Map<String, String>? = null): HttpResponseData {
        return try {
            val finalUrl = buildUrlWithQueries(url, queries)
            val request = Request.Builder().url(finalUrl).delete().apply {
                headers?.forEach { (k, v) -> addHeader(k, v) }
            }.build()
            executeSync(request)
        } catch (e: Exception) {
            HttpResponseData.failure("DELETE request failed: ${e.message}")
        }
    }

    //endregion

    //region ========== 异步请求 ==========

    fun getAsync(url: String, queries: Map<String, String>? = null, headers: Map<String, String>? = null, callback: HttpCallback) {
        try {
            val request = buildGetRequest(url, queries, headers)
            enqueueAsync(request, callback)
        } catch (e: Exception) {
            callback(HttpResponseData.failure("GET async request failed: ${e.message}"), e)
        }
    }

    fun downloadFileAsync(url: String, saveToFile: File, queries: Map<String, String>? = null, headers: Map<String, String>? = null, callback: HttpCallback) {
        try {
            val request = buildGetRequest(url, queries, headers)
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback(HttpResponseData.failure("Download failed: ${e.message}"), e)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use { resp ->
                            if (resp.isSuccessful) {
                                resp.body?.byteStream().use { input ->
                                    saveToFile.outputStream().use { output ->
                                        input?.copyTo(output) ?: throw IOException("Response body is null")
                                    }
                                }
                                callback(HttpResponseData.success(resp.code, resp.headers.toMultimap(), "File downloaded successfully"), null)
                            } else {
                                callback(HttpResponseData.failure("HTTP ${resp.code}: ${resp.message}", resp.code), null)
                            }
                        }
                    } catch (ex: Exception) {
                        callback(HttpResponseData.failure("Download failed: ${ex.message}"), ex)
                    }
                }
            })
        } catch (e: Exception) {
            callback(HttpResponseData.failure("Download async request failed: ${e.message}"), e)
        }
    }

    fun postJsonAsync(url: String, jsonBody: String, headers: Map<String, String>? = null, callback: HttpCallback) {
        try {
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = buildPostRequest(url, requestBody, headers)
            enqueueAsync(request, callback)
        } catch (e: Exception) {
            callback(HttpResponseData.failure("POST JSON async request failed: ${e.message}"), e)
        }
    }

    fun postFormAsync(url: String, formParams: Map<String, String>, headers: Map<String, String>? = null, callback: HttpCallback) {
        try {
            val formBody = FormBody.Builder().apply {
                formParams.forEach { (k, v) -> add(k, v) }
            }.build()
            val request = buildPostRequest(url, formBody, headers)
            enqueueAsync(request, callback)
        } catch (e: Exception) {
            callback(HttpResponseData.failure("POST form async request failed: ${e.message}"), e)
        }
    }

    fun postMultipartAsync(url: String, formFields: Map<String, String>? = null, fileParams: Map<String, File>? = null, headers: Map<String, String>? = null, callback: HttpCallback) {
        try {
            val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                formFields?.forEach { (k, v) -> addFormDataPart(k, v) }
                fileParams?.forEach { (fieldName, file) ->
                    val fileBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    addFormDataPart(fieldName, file.name, fileBody)
                }
            }.build()
            val request = buildPostRequest(url, multipartBody, headers)
            enqueueAsync(request, callback)
        } catch (e: Exception) {
            callback(HttpResponseData.failure("POST multipart async request failed: ${e.message}"), e)
        }
    }

    fun putJsonAsync(url: String, jsonBody: String, headers: Map<String, String>? = null, callback: HttpCallback) {
        try {
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder().url(url).put(requestBody).apply {
                headers?.forEach { (k, v) -> addHeader(k, v) }
            }.build()
            enqueueAsync(request, callback)
        } catch (e: Exception) {
            callback(HttpResponseData.failure("PUT JSON async request failed: ${e.message}"), e)
        }
    }

    fun deleteAsync(url: String, queries: Map<String, String>? = null, headers: Map<String, String>? = null, callback: HttpCallback) {
        try {
            val finalUrl = buildUrlWithQueries(url, queries)
            val request = Request.Builder().url(finalUrl).delete().apply {
                headers?.forEach { (k, v) -> addHeader(k, v) }
            }.build()
            enqueueAsync(request, callback)
        } catch (e: Exception) {
            callback(HttpResponseData.failure("DELETE async request failed: ${e.message}"), e)
        }
    }

    //endregion

    // ===============SSE功能================
    // 在 HttpClient 类中添加以下方法：

    /**
     * 建立 SSE 连接
     * @param url SSE 端点 URL
     * @param headers 请求头
     * @param listener 事件监听器
     * @return SSE 连接管理器
     */
    fun connectSse(
        url: String,
        headers: Map<String, String>? = null,
        listener: com.fanekit.common.util.net.http.SseEventListener
    ): SseConnection {
        val request = Request.Builder().url(url).apply {
            // 设置 SSE 必要的请求头
            addHeader("Accept", "text/event-stream")
            addHeader("Cache-Control", "no-cache")
            headers?.forEach { (k, v) -> addHeader(k, v) }
        }.build()

        val eventSourceListener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                listener.onOpen()
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                listener.onMessage(data, id, type)
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                listener.onError(t ?: RuntimeException("SSE connection failed"))
            }

            override fun onClosed(eventSource: EventSource) {
                listener.onClosed()
            }
        }

        val eventSource = EventSources.createFactory(client)
            .newEventSource(request, eventSourceListener)

        return SseConnection(eventSource)
    }

    /**
     * 建立 SSE 连接（简化版本，直接传递回调函数）
     */
    fun connectSse(
        url: String,
        headers: Map<String, String>? = null,
        onOpen: () -> Unit = {},
        onMessage: (data: String, id: String?, type: String?) -> Unit,
        onError: (error: Throwable) -> Unit = {},
        onClosed: () -> Unit = {}
    ): SseConnection {
        return connectSse(url, headers, object : com.fanekit.common.util.net.http.SseEventListener {
            override fun onOpen() = onOpen()
            override fun onMessage(data: String, id: String?, type: String?) = onMessage(data, id, type)
            override fun onError(error: Throwable) = onError(error)
            override fun onClosed() = onClosed()
        })
    }
    //endregion

    //region ========== 私有工具函数 ==========

    private fun executeSync(request: Request): HttpResponseData {
        return try {
            client.newCall(request).execute().use { response ->
                buildResponseData(response)
            }
        } catch (e: Exception) {
            HttpResponseData.failure("Request execution failed: ${e.message}")
        }
    }

    private fun enqueueAsync(request: Request, callback: HttpCallback) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(HttpResponseData.failure("Request failed: ${e.message}"), e)
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { resp ->
                        callback(buildResponseData(resp), null)
                    }
                } catch (ex: Exception) {
                    callback(HttpResponseData.failure("Response processing failed: ${ex.message}"), ex)
                }
            }
        })
    }

    private fun buildUrlWithQueries(url: String, queries: Map<String, String>?): HttpUrl {
        val builder = url.toHttpUrlOrNull()?.newBuilder() ?: throw IllegalArgumentException("URL is not valid: $url")
        queries?.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder.build()
    }

    private fun buildGetRequest(url: String, queries: Map<String, String>?, headers: Map<String, String>?): Request {
        val finalUrl = buildUrlWithQueries(url, queries)
        return Request.Builder().url(finalUrl).apply {
            headers?.forEach { (k, v) -> addHeader(k, v) }
        }.build()
    }

    private fun buildPostRequest(url: String, body: RequestBody, headers: Map<String, String>?): Request {
        return Request.Builder().url(url).post(body).apply {
            headers?.forEach { (k, v) -> addHeader(k, v) }
        }.build()
    }

    private fun buildResponseData(response: Response): HttpResponseData {
        val code = response.code
        val headerMap = response.headers.toMultimap()
        val bodyString = response.body?.string()

        return if (response.isSuccessful) {
            HttpResponseData.success(code, headerMap, bodyString)
        } else {
            HttpResponseData.failure("HTTP $code: ${response.message}", code)
        }
    }

    //endregion
}
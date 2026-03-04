package com.fanekit.common.api.docs.config

import com.fanekit.common.core.api.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.WebFilter
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.io.IOException

@Component
class DocumentationAccessFilter : WebFilter {
    @Value("\${knife4j.enable:false}")
    private val knife4jEnabled: Boolean = false

    private val log = LoggerFactory.getLogger(DocumentationAccessFilter::class.java)

    @Throws(IOException::class)
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (knife4jEnabled) {
            // 文档功能启用时直接放行
            return chain.filter(exchange)
        }

        // 记录被拦截的文档访问请求
        log.warn("文档访问被拒绝 - knife4j.enable=false: {}", exchange.request.uri)

        // 创建标准的 ApiResult 404 错误响应
        val error: ApiResponse = ApiResponse.notFound()

        // 设置 404 状态码和响应头
        exchange.response.statusCode = HttpStatus.NOT_FOUND
        exchange.response.headers.contentType = MediaType.APPLICATION_JSON

        // 将 ApiResult 序列化为 JSON 并返回
        val jsonResponse = objectMapper.writeValueAsString(error)

        return exchange.response.writeWith(Mono.just(exchange.response.bufferFactory().wrap(jsonResponse.toByteArray())))
    }

    companion object {
        private val objectMapper: ObjectMapper = ObjectMapper().apply {
            // 注册 JavaTimeModule 以支持 Java 8 时间类型
            registerModule(JavaTimeModule())
            // 禁用将日期写为时间戳的默认行为
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}

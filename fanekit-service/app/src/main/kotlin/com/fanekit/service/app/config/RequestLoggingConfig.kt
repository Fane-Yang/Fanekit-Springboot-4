//package com.fanekit.service.app.config
//
//import org.reactivestreams.Publisher
//import org.springframework.core.io.buffer.DataBuffer
//import org.springframework.core.io.buffer.DataBufferUtils
//import org.springframework.http.server.reactive.ServerHttpRequest
//import org.springframework.http.server.reactive.ServerHttpRequestDecorator
//import org.springframework.http.server.reactive.ServerHttpResponse
//import org.springframework.http.server.reactive.ServerHttpResponseDecorator
//import org.springframework.web.server.ServerWebExchange
//import org.springframework.web.server.WebFilter
//import org.springframework.web.server.WebFilterChain
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//import java.nio.charset.StandardCharsets
//import java.util.function.Consumer
//
//
//class RequestResponseLoggingFilter : WebFilter {
//    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
//        // 1. 记录请求
//        val decoratedRequest: ServerHttpRequest = object : ServerHttpRequestDecorator(exchange.getRequest()) {
//            override fun getBody(): Flux<DataBuffer> {
//                return super.getBody().doOnNext(Consumer { dataBuffer: DataBuffer ->
//                    // 复制缓冲区内容以便重复读取
//                    val bytes = ByteArray(dataBuffer!!.readableByteCount())
//                    dataBuffer.read(bytes)
//                    DataBufferUtils.release(dataBuffer)
//                    val body = String(bytes, StandardCharsets.UTF_8)
//                    println("Request Body: " + body)
//                })
//            }
//        }
//
//        // 2. 记录响应
//        val decoratedResponse: ServerHttpResponse = object : ServerHttpResponseDecorator(exchange.getResponse()) {
//            public override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
//                return super.writeWith(Flux.from(body).map({ dataBuffer ->
//                    // 复制缓冲区内容以便重复读取
//                    val bytes = ByteArray(dataBuffer.readableByteCount())
//                    dataBuffer.read(bytes)
//                    DataBufferUtils.release(dataBuffer)
//                    val responseBody = String(bytes, StandardCharsets.UTF_8)
//                    println("Response Body: " + responseBody)
//                    exchange.getResponse().bufferFactory().wrap(bytes)
//                }))
//            }
//        }
//
//        return chain.filter(
//            exchange.mutate()
//                .request(decoratedRequest)
//                .response(decoratedResponse)
//                .build()
//        )
//    }
//}
//

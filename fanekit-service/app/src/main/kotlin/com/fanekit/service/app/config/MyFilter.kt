package com.fanekit.service.app.config

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder
import cn.dev33.satoken.stp.StpUtil
import com.fanekit.common.core.api.ApiResponse
import com.fanekit.common.core.constants.ErrorCodes
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono


/**
 * 自定义过滤器
 */
@Component
class MyFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        println("进入自定义过滤器")

        try {
            StpUtil.checkLogin()
        } catch (e: Exception) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            ApiResponse.error(ErrorCodes.TOKEN_INVALID, ErrorCodes.defaultMessages[ErrorCodes.TOKEN_INVALID])
            return chain.filter(exchange)
        }

        try {
            // 先 set 上下文，再调用 Sa-Token 同步 API，并在 finally 里清除上下文
            SaReactorSyncHolder.setContext(exchange)
            println(StpUtil.isLogin())

        } finally {
            SaReactorSyncHolder.clearContext()
        }
        return chain.filter(exchange)
    }
}
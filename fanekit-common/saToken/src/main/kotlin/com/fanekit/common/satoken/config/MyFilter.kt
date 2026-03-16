//package com.fanekit.common.satoken.config
//
//import cn.dev33.satoken.reactor.context.SaReactorSyncHolder
//import cn.dev33.satoken.stp.StpUtil
//import org.springframework.stereotype.Component
//import org.springframework.web.server.ServerWebExchange
//import org.springframework.web.server.WebFilter
//import org.springframework.web.server.WebFilterChain
//import reactor.core.publisher.Mono
//
//
///**
// * 自定义过滤器,会被全局过滤器包裹，用户可以二次开发
// */
//@Component
//class MyFilter : WebFilter {
//    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
//        println("进入自定义过滤器")
//
//        try {
//            // 先 set 上下文，再调用 Sa-Token 同步 API，并在 finally 里清除上下文
//            SaReactorSyncHolder.setContext(exchange)
//            println(StpUtil.isLogin())
//        } finally {
//            SaReactorSyncHolder.clearContext()
//        }
//
//        return chain.filter(exchange)
//    }
//}
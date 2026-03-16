package com.fanekit.common.satoken.config

//import cn.dev33.satoken.`fun`.SaFunction
//import cn.dev33.satoken.reactor.filter.SaReactorFilter
//import cn.dev33.satoken.router.SaRouter
//import cn.dev33.satoken.stp.StpUtil
//import cn.dev33.satoken.util.SaResult
//import com.fanekit.common.core.api.ApiResponse
//import com.fanekit.common.core.constants.ErrorCodes
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//
// // 复制此内容到服务中，自定义返回内容和异常处理
//@Configuration
//open class SaTokenConfigure {
//    /**
//     * 注册 [Sa-Token全局过滤器]
//     */
//    @Bean
//    open fun saReactorFilter(): SaReactorFilter = SaReactorFilter()
//        // 指定 [拦截路由]（此处为拦截所有path）
//        .addInclude("/**")
//        // 指定 [放行路由]
//        .addExclude("/favicon.ico")
//        // 指定[认证函数]: 每次请求执行
//        .setAuth {
//            println("---------- sa全局认证")
//            StpUtil.checkLogin()
////             SaRouter.match("/test/test", SaFunction { StpUtil.checkLogin() })
//        }
//        // 指定[异常处理函数]：每次[认证函数]发生异常时执行此函数
//        .setError { e: Throwable ->
//            println("---------- sa全局异常 ")
//            ApiResponse.error(ErrorCodes.TOKEN_INVALID, ErrorCodes.defaultMessages[ErrorCodes.TOKEN_INVALID])
//        }
//}

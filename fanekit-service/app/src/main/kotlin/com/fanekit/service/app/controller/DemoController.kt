package com.fanekit.service.app.controller


import cn.dev33.satoken.`fun`.SaRetGenericFunction
import cn.dev33.satoken.reactor.context.SaReactorHolder
import cn.dev33.satoken.stp.StpUtil
import cn.dev33.satoken.util.SaResult
import com.fanekit.common.core.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


@RestController
@RequestMapping("/api/demo")
class DemoController(
//    private val testService: TestService
) {

    @GetMapping("/hello")
    fun hello(): Map<String, String> {
//        testService.test()
        return mapOf(
            "status" to "OK",
            "service" to "DemoController",
            "timestamp" to System.currentTimeMillis().toString()
        )
    }

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "OK",
            "service" to "DemoController",
            "timestamp" to System.currentTimeMillis().toString()
        )
    }

    @PostMapping("doLogin")
    fun doLogin(): Mono<ApiResponse> {
        // 此处仅作模拟示例，真实项目需要从数据库中查询数据进行比对
        return SaReactorHolder.sync( {
            StpUtil.login(10001)
            ApiResponse.success("登录成功!")
        })
    }

    // 查询登录状态，浏览器访问： http://localhost:8081/user/isLogin
    @GetMapping("isLogin")
    fun isLogin(): Mono<ApiResponse> {
        return SaReactorHolder.sync {
            ApiResponse.success("当前会话是否登录：" + StpUtil.isLogin())
        }
    }
}

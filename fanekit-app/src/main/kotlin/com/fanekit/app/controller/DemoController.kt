package com.fanekit.app.controller


import org.springframework.web.bind.annotation.*

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
}

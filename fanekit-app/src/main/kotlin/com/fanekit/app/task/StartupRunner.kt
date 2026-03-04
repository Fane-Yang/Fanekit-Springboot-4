package com.fanekit.app.task

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component


@Component
class StartupRunner(private val applicationContext: ApplicationContext) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        // 在程序启动时执行的代码
        val log = LoggerFactory.getLogger(StartupRunner::class.java)
        log.info("程序启动完成，执行初始化逻辑...")
        val env = applicationContext.environment.activeProfiles[0]
        if (mutableListOf("dev", "local", "test").contains(env)) {
            log.info("当前运行环境: 测试环境: $env")
        } else {
            log.info("当前运行环境: 生产环境: $env")
        }
    }
}
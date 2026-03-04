package com.fanekit.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.fanekit"])
class AppApplication

fun main(args: Array<String>) {
    runApplication<AppApplication>(*args)
}

package com.fanekit.service.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.HttpLogging

@SpringBootApplication
@ComponentScan(basePackages = ["com.fanekit"])
class AppApplication

fun main(args: Array<String>) {
    runApplication<AppApplication>(*args)
}

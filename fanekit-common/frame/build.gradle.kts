plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common.frame"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-frame")
}
dependencies {
    api(libs.spring.boot.starter.webflux)
    api(libs.spring.boot.configuration.processor)
    api(libs.jackson.module.kotlin)
    api(libs.reactor.kotlin.extensions)
    api(libs.kotlin.reflect)
    api(libs.kotlinx.coroutines.reactor)
}
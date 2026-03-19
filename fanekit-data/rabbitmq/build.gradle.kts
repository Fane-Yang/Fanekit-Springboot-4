plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.data.rabbitmq"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-data-rabbitmq")
}

dependencies {
    // Spring Boot AMQP (RabbitMQ) Related
    api(libs.spring.boot.starter.amqp)

    // FastJSON2 Serialization
    api(libs.fastjson2)
    api(libs.fastjson2.kotlin)

    // Jackson for RabbitMQ Message Converter
//    implementation(libs.jackson.module.kotlin)

    // Spring Boot 自动装配相关
    implementation(libs.spring.boot.autoconfigure)

    // Kotlin 相关
    implementation(libs.kotlin.reflect)

    // 基础依赖
    implementation(libs.logback)
}

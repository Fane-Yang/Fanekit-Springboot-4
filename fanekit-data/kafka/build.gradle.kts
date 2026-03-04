plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.data.kafka"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-data-kafka")
}

dependencies {
    // Spring Boot Kafka 相关
//    api(libs.spring.kafka)
    api(libs.spring.boot.starter.kafka)
//    api(libs.kafka.clients)
    // FastJSON2 序列化
    api(libs.fastjson2)
    api(libs.fastjson2.kotlin)
    // Spring Boot 自动装配相关
    implementation(libs.spring.boot.autoconfigure)

    // Kotlin 相关
    implementation(libs.kotlin.reflect)

    // 基础依赖
    implementation(libs.logback)
}

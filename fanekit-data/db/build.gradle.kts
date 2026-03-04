plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.data.db"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-data-db")
}

dependencies {

    // MyBatis Flex 相关
    api(libs.mybatis.flex.spring.boot4.starter)
    api(libs.spring.boot.starter.jdbc)
    api(libs.mybatis.flex.kotlin.extensions)


    // 连接池
    api(libs.hikariCP)

    // Spring Boot 自动装配相关
    implementation(libs.spring.boot.autoconfigure)


    implementation(libs.jdbc.mysql)
    implementation(libs.jdbc.postgresql)

    // Kotlin 相关
    implementation(libs.kotlin.reflect)
    // 基础依赖
    implementation(libs.logback)
}
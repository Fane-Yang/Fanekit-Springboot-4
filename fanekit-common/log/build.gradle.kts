plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common.log"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-log")
}

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.logging)
    implementation(libs.slf4j)
    implementation(libs.lombok)
    // 基础依赖
    implementation(libs.logback)

    // Compile-time only Jakarta Servlet API to satisfy CommonsRequestLoggingFilter's supertype (Filter)
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
}

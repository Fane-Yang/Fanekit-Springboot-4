plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common.core"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-core")
}

dependencies {
    // 基础依赖
    api(libs.logback)

    // Kotlin 反射支持
    api(libs.kotlin.reflect)
}
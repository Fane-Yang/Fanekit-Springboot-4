plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common.saToken"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-saToken")
}
dependencies {
    // 基础依赖
    api(libs.spring.boot.starter.webflux)
    api(libs.spring.boot.configuration.processor)
    implementation(projects.fanekitCommon.core)
//    api(projects.fanekitCommon.frame)
    // 日志
    implementation(libs.logback)// 所有子依赖禁止再次依赖
    // 协程
    implementation(libs.kotlinx.coroutines.core)
    // sa-token
    api(libs.sa.token.reactor.spring.boot4.starter)
    api(libs.sa.token.redis.template)
    api(libs.commons.pool2)
}

tasks.test {
    useJUnitPlatform()
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common.api.docs"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-api-docs")
}

dependencies {

    // https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-starter-webflux-ui
    implementation(libs.spring.boot.starter.webflux)
    api("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.1")
    // knife4j-API文档的UI
    // https://mvnrepository.com/artifact/com.github.xiaoymin/knife4j-openapi3-ui
    api("com.github.xiaoymin:knife4j-openapi3-ui:4.5.0")
    implementation(libs.slf4j)
    implementation(projects.fanekitCommon.core)
}

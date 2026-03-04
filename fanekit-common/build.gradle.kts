plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common")
}

dependencies {
    // 聚合所有子模块
    api(projects.fanekitCommon.core)
    api(projects.fanekitCommon.util)
    api(projects.fanekitCommon.excel)
    api(projects.fanekitCommon.ip)
    api(projects.fanekitCommon.quartz)
    // 下面模块都有spring的东西
    api(projects.fanekitCommon.frame)
    api(projects.fanekitCommon.log)
    api(projects.fanekitCommon.apiDocs)
    api(projects.fanekitCommon.saToken)
}
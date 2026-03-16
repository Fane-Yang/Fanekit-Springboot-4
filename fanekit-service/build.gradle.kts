plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.service"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-service")
}
dependencies {
    // 聚合所有子模块
//    api(projects.fanekitData.db)
//    api(projects.fanekitData.kafka)
//    api(projects.fanekitData.redis)
    api(projects.fanekitService.app)
}
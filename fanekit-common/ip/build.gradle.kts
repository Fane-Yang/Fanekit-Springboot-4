plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common.ip"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-ip")
}
dependencies {
    // 基础依赖
    implementation(libs.logback)// 所有子依赖禁止再次依赖
    // 依赖核心模块，并且禁止二次依赖
    implementation("com.maxmind.geoip2:geoip2:4.3.1")
    // ip2region：如用官方 jar 或 maven central 可按实际坐标引入
     implementation("org.lionsoul:ip2region:2.7.0") // 仅示例
}
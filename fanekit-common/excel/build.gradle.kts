plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common.excel"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-excel")
}
dependencies {
    // 基础依赖
    implementation(libs.logback)// 所有子依赖禁止再次依赖

    // Apache POI - Excel文件处理
    implementation(libs.poi)
    implementation(libs.poi.ooxml)
    implementation(libs.poi.scratchpad)
}
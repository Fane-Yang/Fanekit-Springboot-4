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
    // 依赖核心模块，并且禁止二次依赖
//    implementation(projects.fanekitCommon.core)

    // Apache POI - Excel文件处理
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("org.apache.poi:poi-scratchpad:5.4.1")
}
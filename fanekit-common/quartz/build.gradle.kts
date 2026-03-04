plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.fanekit.common.quartz"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-quartz")
}
dependencies {
    // 基础依赖
    // 日志
    implementation(libs.logback)// 所有子依赖禁止再次依赖
    // 协程
    implementation(libs.kotlinx.coroutines.core)
    // Quartz
    implementation(libs.quartz)

    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

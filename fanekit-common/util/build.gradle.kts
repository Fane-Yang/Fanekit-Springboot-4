plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}


group = "com.fanekit.common.util"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-common-util")
}

dependencies {
    // 依赖核心模块
    api(projects.fanekitCommon.core)

    // fastjson2依赖 依赖此模块的模块可以直接使用 fastjson2
    api(libs.fastjson2)

    // 协程
    api(libs.kotlinx.coroutines.core)

    // jwt
    api(libs.auth0.java.jwt)

    // OKHttp3
    implementation(libs.okhttp)

    // OKHttp3 SSE
    implementation(libs.okhttp.sse)

    // 对象连接池库
    implementation(libs.commons.pool2)

    // sshj - SSH 客户端库
    implementation(libs.sshj)

    // zip4j - 支持密码的ZIP工具库
    implementation(libs.zip4j)

    // BouncyCastle 加密库 (用于 SM2/SM3 等国密算法)
    implementation(libs.bouncycastle.bcpkix.jdk18on)
    implementation(libs.bouncycastle.bcprov.jdk18on)


    // Google zxing - 二维码生成库 TODO jar600k后续可以看看按需集成
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")
    // 测试依赖
//    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
//    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
//    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}


//tasks.shadowJar {
//    relocate("org.bouncycastle", "com.fanekit.shaded.bouncycastle")
//}
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springboot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.fanekit.app"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-app")
}

tasks.bootJar {
    // 设置重复处理策略，排除重复的条目
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("fanekit-app")
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}


dependencies {

    implementation(projects.fanekitCommon)
    implementation(projects.fanekitCommon.frame)
    implementation(projects.fanekitCommon.core)
//    implementation(projects.fanekitComponents)
    implementation(projects.fanekitCommon.excel)
    implementation(projects.fanekitCommon.quartz)
    implementation(projects.fanekitCommon.ip)

    implementation(projects.fanekitCommon.apiDocs)
    implementation(projects.fanekitCommon.log)
    implementation(projects.fanekitData.db)
    implementation(projects.fanekitData.kafka)
    implementation(projects.fanekitData.redis)

    // Lombok（编译期使用，运行时不会影响）
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    // Spring Boot 配置元数据
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

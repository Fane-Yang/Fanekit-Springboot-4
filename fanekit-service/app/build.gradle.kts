import org.gradle.api.tasks.bundling.*
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springboot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.fanekit.service.app"
version = "1.0.0"

tasks.jar {
    // 设置生成的 JAR 文件名
    archiveBaseName.set("fanekit-service-app")
}

tasks.bootJar {
    // 设置重复处理策略，排除重复的条目
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("fanekit-service-app")
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
    implementation(projects.fanekitCommon.saToken)

    implementation(projects.fanekitCommon.apiDocs)
    implementation(projects.fanekitCommon.log)
    implementation(projects.fanekitData.db)
    implementation(projects.fanekitData.kafka)
    implementation(projects.fanekitData.redis)
    implementation(projects.fanekitData.rabbitmq)


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

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true // 确保 bootJar 任务启用
    archiveClassifier.set("exec") // 为默认的 fat jar 添加 classifier，例如 my-app-1.0.0-exec.jar
}

// 创建一个只包含主类代码的 JAR 任务
val plainJar by tasks.registering(Jar::class) {
    dependsOn(tasks.classes) // 确保编译完成后再打包
    archiveClassifier.set("") // 不设置 classifier，这样名称就是 my-app-1.0.0.jar

    from(sourceSets.main.get().output) {
        // 排除掉 META-INF/MANIFEST.MF，因为每个 jar 只能有一个主清单
        exclude("META-INF/MANIFEST.MF")
    }
}

// 创建一个用于复制依赖的 task
val copyLibs by tasks.registering(Copy::class) {
    from(configurations.runtimeClasspath) // 从 runtime 依赖中获取所有 jar
    into(layout.buildDirectory.dir("libs")) // 将它们复制到 build/libs 目录
}

// 创建一个用于复制配置文件的 task
val copyConfig by tasks.registering(Copy::class) {
    // 假设您的配置文件在 src/main/resources 或单独的 config 目录
    from(sourceSets.main.get().resources)
    // 这里可以根据需要过滤，只复制 .properties, .yml, .yaml 等配置文件
    include("**/*.properties", "**/*.yml", "**/*.yaml", "**/*.xml")
    into(layout.buildDirectory.dir("config")) // 将它们复制到 build/config 目录
}

// 创建一个组合任务，一键执行所有步骤
val packageApp by tasks.registering {
    group = "build" // 将此任务归类到 build 组
    description = "Builds the application with separated libs and config"

    dependsOn("plainJar", "copyLibs", "copyConfig")
}
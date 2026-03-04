
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "FaneKit-SpringBoot"


// 包含子模块
include(
    // ---- common 通用工具模块 -------------
    // ---- 核心模块 -------------
    ":fanekit-common:core",
    // ---- 工具类模块 -------------
    ":fanekit-common:util",
    // ---- 独立Excel工具模块 -------------
    ":fanekit-common:excel",
    // ---- 独立IP处理工具模块 -------------
    ":fanekit-common:ip",
    // ---- 独立定时任务工具模块 -------------
    ":fanekit-common:quartz",
    // ---- 框架基础模块 -------------
    ":fanekit-common:frame",
    // ---- 日志模块 -------------
    ":fanekit-common:log",
    // ---- API文档模块 -------------
    ":fanekit-common:api-docs",
    ":fanekit-common:saToken",

    // ---- data 数据库工具类的核心实现，用户层面无需关注，除非有BUG需要修复等问题 ---------------
    ":fanekit-data:db",
    ":fanekit-data:redis",
    ":fanekit-data:rabbitmq",
    ":fanekit-data:kafka",

    // ---- components 独立工具模块 ---------
    // ---- 复杂的定时任务模块，支持记录定时任务执行日志等 ---------
//    ":fanekit-components:quartz",

    // ---- app 业务模块(后续考虑拆分为API模块、业务逻辑模块等细分模块) -----------
    ":fanekit-app",
)

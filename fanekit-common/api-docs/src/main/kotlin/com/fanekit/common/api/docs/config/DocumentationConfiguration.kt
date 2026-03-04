package com.fanekit.common.api.docs.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import reactor.core.publisher.Mono

@Configuration
class DocumentationConfiguration(private val environment: Environment) {

    private val log = LoggerFactory.getLogger(DocumentationConfiguration::class.java)

    @Value("\${app.version:1.0.0}")
    private val version: String? = null

    @Value("\${server.port:8080}")
    private val serverPort: String? = null

    @Value("\${server.servlet.context-path:}")
    private val contextPath: String? = null

    @Value("\${knife4j.enable:false}")
    private val knife4jEnabled: Boolean = false

    @Value("\${spring.webflux.base-path:}")
    private val webfluxBasePath: String? = null

    /**
     * OpenAPI 配置 Bean - 仅在文档启用时创建
     */
    @Bean
    fun customOpenAPI(): OpenAPI? {
        return if (!knife4jEnabled) {
            null
        } else {
            OpenAPI().info(
                Info()
                    .title("FaneKit API")
                    .version(version)
                    .description("FaneKit接口文档")
                    .license(License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
            )
        }
    }

    /**
     * 应用启动完成后的文档状态提示
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        if (knife4jEnabled) {
            printDocumentationUrls()
        }
    }

    /**
     * 打印文档访问地址
     */
    private fun printDocumentationUrls() {
        try {
            val baseUrl = buildBaseUrl()
            val prefix = if (!webfluxBasePath.isNullOrBlank()) webfluxBasePath else ""

            log.info("===========================================")
            log.info("📖 API文档已启用，访问地址如下：")
            log.info("🔗 Knife4j文档: {}{}/doc.html", baseUrl, prefix)
            log.info("🔗 Swagger UI: {}{}/swagger-ui/index.html", baseUrl, prefix)
            log.info("🔗 OpenAPI JSON: {}{}/v3/api-docs", baseUrl, prefix)
            log.info("===========================================")
        } catch (e: Exception) {
            log.warn("无法获取文档访问地址: {}", e.message)
        }
    }

    /**
     * 构建基础访问URL
     */
    private fun buildBaseUrl(): String {
        val activeProfiles = environment.activeProfiles
        val profile = if (activeProfiles.isNotEmpty()) activeProfiles[0] else "default"

        var protocol = "http"
        var host = "localhost"

        if ("prod" == profile) {
            val serverHost = environment.getProperty("server.host")
            if (!serverHost.isNullOrEmpty()) {
                host = serverHost
            }

            val sslEnabled = environment.getProperty("server.ssl.enabled", Boolean::class.java)
            if (true == sslEnabled) {
                protocol = "https"
            }
        }

        val baseUrl = StringBuilder()
        baseUrl.append(protocol).append("://").append(host)

        if (serverPort != "80" && serverPort != "443") {
            baseUrl.append(":").append(serverPort)
        }

        if (!contextPath.isNullOrEmpty() && contextPath != "/") {
            if (!contextPath.startsWith("/")) {
                baseUrl.append("/")
            }
            baseUrl.append(contextPath)
        }

        return baseUrl.toString()
    }
}

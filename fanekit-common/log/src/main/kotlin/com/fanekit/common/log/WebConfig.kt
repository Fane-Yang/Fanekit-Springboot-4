package com.fanekit.common.log

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer


@Configuration
class WebConfig : WebFluxConfigurer {
    @Bean
    fun requestResponseLoggingFilter(): RequestResponseLoggingFilter {
        return RequestResponseLoggingFilter()
    }
}

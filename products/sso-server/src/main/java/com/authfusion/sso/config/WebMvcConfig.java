package com.authfusion.sso.config;

import com.authfusion.sso.cc.ToeScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@ToeScope(value = "Web MVC 설정", sfr = {})
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${authfusion.sso.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${authfusion.sso.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${authfusion.sso.cors.max-age:3600}")
    private long maxAge;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(maxAge);
    }
}

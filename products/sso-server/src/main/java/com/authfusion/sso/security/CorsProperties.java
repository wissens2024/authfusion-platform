package com.authfusion.sso.security;

import com.authfusion.sso.cc.ToeScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ToeScope(value = "CORS 설정", sfr = {"FDP_ACC.1"})
@Configuration
@ConfigurationProperties(prefix = "authfusion.sso.cors")
@Getter
@Setter
public class CorsProperties {

    private String allowedOrigins = "http://localhost:3000";
    private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
    private long maxAge = 3600;
}

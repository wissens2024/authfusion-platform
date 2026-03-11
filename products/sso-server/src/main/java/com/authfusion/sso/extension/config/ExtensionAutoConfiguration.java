package com.authfusion.sso.extension.config;

import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.extension.ExtensionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ExtendedFeature("확장 자동 설정")
@Configuration
@ConditionalOnExtendedMode
@EnableConfigurationProperties(ExtensionProperties.class)
public class ExtensionAutoConfiguration {

    @Bean
    public ExtensionRegistry extensionRegistry() {
        return new ExtensionRegistry();
    }
}

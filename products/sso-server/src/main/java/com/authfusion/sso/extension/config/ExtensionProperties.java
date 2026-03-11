package com.authfusion.sso.extension.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authfusion.sso.extension")
@Getter
@Setter
public class ExtensionProperties {

    private boolean enabled = true;
    private String scanPackage = "com.authfusion.sso.extension";
}

package com.authfusion.sso.ldap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "authfusion.sso.ldap")
@Getter
@Setter
public class LdapProperties {

    private boolean enabled = false;
    private String url = "ldap://localhost:389";
    private String baseDn = "dc=authfusion,dc=io";
    private String bindDn = "cn=sso-service,dc=authfusion,dc=io";
    private String bindPassword = "";
    private String userSearchBase = "ou=users";
    private String userSearchFilter = "(uid={0})";
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
    private Map<String, String> attributeMapping = new HashMap<>();

    public LdapProperties() {
        attributeMapping.put("username", "uid");
        attributeMapping.put("email", "mail");
        attributeMapping.put("first-name", "givenName");
        attributeMapping.put("last-name", "sn");
    }
}

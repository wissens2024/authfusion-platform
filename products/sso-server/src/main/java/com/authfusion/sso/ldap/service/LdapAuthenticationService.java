package com.authfusion.sso.ldap.service;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.ldap.config.LdapProperties;
import com.authfusion.sso.ldap.model.LdapAuthResult;
import com.authfusion.sso.ldap.model.LdapUserAttributes;
import lombok.extern.slf4j.Slf4j;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Map;

@ToeScope(value = "LDAP 인증 서비스", sfr = {"FIA_UAU.1"})
@Slf4j
public class LdapAuthenticationService {

    private final LdapConnectionService connectionService;
    private final LdapProperties properties;

    public LdapAuthenticationService(LdapConnectionService connectionService, LdapProperties properties) {
        this.connectionService = connectionService;
        this.properties = properties;
    }

    /**
     * Search-then-bind LDAP 인증
     * 1. 서비스 계정으로 사용자 DN 검색
     * 2. 찾은 DN으로 사용자 비밀번호 바인드 검증
     */
    public LdapAuthResult authenticate(String username, String password) {
        DirContext bindCtx = null;
        DirContext userCtx = null;

        try {
            // Step 1: Search for user DN using service account
            bindCtx = connectionService.createBindConnection();

            String searchFilter = properties.getUserSearchFilter().replace("{0}", escapeFilter(username));
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(getReturnAttributes());

            NamingEnumeration<SearchResult> results = bindCtx.search(
                    properties.getUserSearchBase(), searchFilter, controls);

            if (!results.hasMoreElements()) {
                log.debug("LDAP user not found: {}", username);
                return LdapAuthResult.builder()
                        .success(false)
                        .errorMessage("User not found in LDAP")
                        .build();
            }

            SearchResult result = results.nextElement();
            String userDn = result.getNameInNamespace();
            Attributes attrs = result.getAttributes();

            // Step 2: Bind as user to verify password
            userCtx = connectionService.createUserConnection(userDn, password);

            // Authentication succeeded - extract attributes
            LdapUserAttributes userAttrs = mapAttributes(attrs, userDn);

            log.info("LDAP authentication successful for user '{}'", username);
            return LdapAuthResult.builder()
                    .success(true)
                    .userAttributes(userAttrs)
                    .build();

        } catch (javax.naming.AuthenticationException e) {
            log.debug("LDAP authentication failed for user '{}': invalid credentials", username);
            return LdapAuthResult.builder()
                    .success(false)
                    .errorMessage("Invalid LDAP credentials")
                    .build();
        } catch (NamingException e) {
            log.error("LDAP operation failed for user '{}': {}", username, e.getMessage());
            return LdapAuthResult.builder()
                    .success(false)
                    .errorMessage("LDAP error: " + e.getMessage())
                    .build();
        } finally {
            closeContext(bindCtx);
            closeContext(userCtx);
        }
    }

    public LdapUserAttributes searchUser(String username) {
        DirContext ctx = null;
        try {
            ctx = connectionService.createBindConnection();

            String searchFilter = properties.getUserSearchFilter().replace("{0}", escapeFilter(username));
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(getReturnAttributes());

            NamingEnumeration<SearchResult> results = ctx.search(
                    properties.getUserSearchBase(), searchFilter, controls);

            if (!results.hasMoreElements()) {
                return null;
            }

            SearchResult result = results.nextElement();
            return mapAttributes(result.getAttributes(), result.getNameInNamespace());
        } catch (NamingException e) {
            log.error("LDAP search failed for user '{}': {}", username, e.getMessage());
            return null;
        } finally {
            closeContext(ctx);
        }
    }

    private LdapUserAttributes mapAttributes(Attributes attrs, String dn) throws NamingException {
        Map<String, String> mapping = properties.getAttributeMapping();
        return LdapUserAttributes.builder()
                .dn(dn)
                .username(getAttributeValue(attrs, mapping.getOrDefault("username", "uid")))
                .email(getAttributeValue(attrs, mapping.getOrDefault("email", "mail")))
                .firstName(getAttributeValue(attrs, mapping.getOrDefault("first-name", "givenName")))
                .lastName(getAttributeValue(attrs, mapping.getOrDefault("last-name", "sn")))
                .externalId(dn)
                .build();
    }

    private String getAttributeValue(Attributes attrs, String attrName) throws NamingException {
        Attribute attr = attrs.get(attrName);
        return attr != null ? (String) attr.get() : null;
    }

    private String[] getReturnAttributes() {
        Map<String, String> mapping = properties.getAttributeMapping();
        return mapping.values().toArray(new String[0]);
    }

    private String escapeFilter(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\5c"); break;
                case '*': sb.append("\\2a"); break;
                case '(': sb.append("\\28"); break;
                case ')': sb.append("\\29"); break;
                case '\0': sb.append("\\00"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private void closeContext(DirContext ctx) {
        if (ctx != null) {
            try { ctx.close(); } catch (NamingException e) { /* ignore */ }
        }
    }
}

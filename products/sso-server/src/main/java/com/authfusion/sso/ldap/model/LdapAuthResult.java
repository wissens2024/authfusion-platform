package com.authfusion.sso.ldap.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LdapAuthResult {
    private boolean success;
    private LdapUserAttributes userAttributes;
    private String errorMessage;
}

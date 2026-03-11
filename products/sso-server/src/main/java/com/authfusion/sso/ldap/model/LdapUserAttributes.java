package com.authfusion.sso.ldap.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LdapUserAttributes {
    private String dn;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String externalId;
}

package com.authfusion.sso.oidc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    private String sub;

    @JsonProperty("preferred_username")
    private String preferredUsername;

    private String email;

    @JsonProperty("email_verified")
    private boolean emailVerified;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    private String name;

    private List<String> roles;
}

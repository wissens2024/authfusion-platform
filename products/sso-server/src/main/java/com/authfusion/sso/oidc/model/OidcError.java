package com.authfusion.sso.oidc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OidcError {

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("error_uri")
    private String errorUri;

    private String state;

    // Standard error codes
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String ACCESS_DENIED = "access_denied";
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
    public static final String INVALID_SCOPE = "invalid_scope";
    public static final String SERVER_ERROR = "server_error";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    public static final String INVALID_CLIENT = "invalid_client";
}

package com.authfusion.sso.oidc.grant;

import com.authfusion.sso.oidc.model.TokenRequest;
import com.authfusion.sso.oidc.model.TokenResponse;

public interface GrantHandler {

    String getGrantType();

    TokenResponse handle(TokenRequest request);
}

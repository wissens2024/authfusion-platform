package com.authfusion.sso.extension.spi;

import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.extension.ExtensionLifecycle;

import java.util.Map;

/**
 * 외부 IdP 확장 SPI (소셜 로그인, 기업 IdP 등).
 */
@ExtendedFeature("외부 IdP 확장 SPI")
public interface IdentityProviderExtension extends ExtensionLifecycle {

    /**
     * IdP 식별자 (e.g., "google", "azure-ad")
     */
    String getProviderId();

    /**
     * 외부 IdP 인가 URL을 생성합니다.
     */
    String getAuthorizationUrl(String state, String redirectUri, Map<String, String> additionalParams);

    /**
     * IdP 콜백을 처리하여 사용자 정보를 반환합니다.
     */
    Map<String, Object> handleCallback(String code, String state, String redirectUri);

    /**
     * IdP 사용자 정보를 로컬 사용자 속성으로 매핑합니다.
     */
    Map<String, String> mapUser(Map<String, Object> idpUserInfo);
}

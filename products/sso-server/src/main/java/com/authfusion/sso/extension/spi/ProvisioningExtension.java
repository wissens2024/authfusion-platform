package com.authfusion.sso.extension.spi;

import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.extension.ExtensionLifecycle;

import java.util.List;
import java.util.Map;

/**
 * 프로비저닝 확장 SPI (SCIM 등).
 */
@ExtendedFeature("프로비저닝 확장 SPI")
public interface ProvisioningExtension extends ExtensionLifecycle {

    /**
     * 사용자 프로비저닝
     */
    Map<String, Object> provisionUser(Map<String, Object> userAttributes);

    /**
     * 사용자 디프로비저닝
     */
    boolean deprovisionUser(String externalUserId);

    /**
     * 사용자 동기화
     */
    List<Map<String, Object>> syncUsers();
}

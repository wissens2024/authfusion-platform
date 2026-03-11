package com.authfusion.sso.extension.spi;

import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.extension.ExtensionLifecycle;

/**
 * 크리덴셜 관리 확장 SPI (KeyHub, Vault 연동 등).
 */
@ExtendedFeature("크리덴셜 확장 SPI")
public interface CredentialExtension extends ExtensionLifecycle {

    /**
     * 크리덴셜 저장
     */
    void storeCredential(String credentialId, byte[] credentialData);

    /**
     * 크리덴셜 조회
     */
    byte[] retrieveCredential(String credentialId);

    /**
     * 크리덴셜 교체
     */
    void rotateCredential(String credentialId);
}

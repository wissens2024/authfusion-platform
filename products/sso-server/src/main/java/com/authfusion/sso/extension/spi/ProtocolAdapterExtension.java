package com.authfusion.sso.extension.spi;

import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.extension.ExtensionLifecycle;

import java.util.List;
import java.util.Map;

/**
 * 프로토콜 어댑터 확장 SPI (SAML 2.0 등).
 */
@ExtendedFeature("프로토콜 어댑터 확장 SPI")
public interface ProtocolAdapterExtension extends ExtensionLifecycle {

    /**
     * 지원하는 프로토콜 이름 (e.g., "saml2", "cas")
     */
    String getProtocol();

    /**
     * 이 프로토콜이 노출하는 엔드포인트 목록
     */
    List<String> getEndpoints();

    /**
     * 프로토콜 요청 처리
     */
    Map<String, Object> handleRequest(String endpoint, Map<String, Object> request);
}

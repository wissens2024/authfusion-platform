package com.authfusion.sso.extension.spi;

import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.extension.ExtensionLifecycle;

import java.util.Map;

/**
 * 인증 확장 SPI (FIDO2, Push 알림 등).
 * GrantHandler 패턴을 따르는 전략 패턴 기반 인터페이스.
 */
@ExtendedFeature("인증 확장 SPI")
public interface AuthenticationExtension extends ExtensionLifecycle {

    /**
     * 이 확장이 지원하는 인증 방식인지 확인합니다.
     */
    boolean supports(String authMethod);

    /**
     * 인증 시작 (챌린지 생성 등)
     * @return 클라이언트에 전달할 챌린지 데이터
     */
    Map<String, Object> initiateAuthentication(String userId, Map<String, Object> context);

    /**
     * 인증 검증 (응답 검증)
     * @return 인증 성공 여부
     */
    boolean verifyAuthentication(String userId, Map<String, Object> response);
}

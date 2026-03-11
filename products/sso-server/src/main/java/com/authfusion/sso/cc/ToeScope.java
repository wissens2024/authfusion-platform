package com.authfusion.sso.cc;

import java.lang.annotation.*;

/**
 * 최소 TOE(Target of Evaluation) 범위에 포함되는 클래스 또는 메소드를 표시합니다.
 * CC(Common Criteria) 인증 시 이 어노테이션이 붙은 코드가 평가 대상입니다.
 *
 * <p>리플렉션 스캔을 통해 TOE 인벤토리를 자동 생성할 수 있습니다.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToeScope {

    /**
     * TOE 구성요소 설명 (예: "OIDC 인가 엔드포인트", "JWT 토큰 서명")
     */
    String value() default "";

    /**
     * 관련 보안기능요구사항 (SFR) 식별자
     * (예: "FIA_UAU.1", "FCS_COP.1", "FAU_GEN.1")
     */
    String[] sfr() default {};
}

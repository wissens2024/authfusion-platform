package com.authfusion.sso.cc;

import java.lang.annotation.*;

/**
 * 확장 기능(Extended Feature)으로 분류되는 클래스를 표시합니다.
 * CC 최소 TOE에는 포함되지 않으며, 확장 모드에서만 활성화됩니다.
 *
 * <p>{@code authfusion.sso.cc.extended-features-enabled=false}일 때
 * 이 어노테이션이 붙은 빈은 {@link ConditionalOnExtendedMode}와 함께
 * 사용하여 비활성화할 수 있습니다.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtendedFeature {

    /**
     * 확장 기능 설명 (예: "클라이언트 관리 API", "RBAC 역할 관리")
     */
    String value() default "";
}

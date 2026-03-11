package com.authfusion.sso.cc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

/**
 * 확장 모드일 때만 빈을 생성하는 조건부 어노테이션입니다.
 *
 * <p>{@code authfusion.sso.cc.extended-features-enabled=true}일 때만
 * 해당 빈이 Spring 컨텍스트에 등록됩니다.</p>
 *
 * <p>{@code matchIfMissing=true}이므로 프로퍼티가 없으면 기본적으로
 * 확장 모드(모든 기능 활성화)로 동작합니다.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(
        name = "authfusion.sso.cc.extended-features-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public @interface ConditionalOnExtendedMode {
}

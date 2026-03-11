package com.authfusion.agent.cc;

import java.lang.annotation.*;

/**
 * 최소 TOE(Target of Evaluation) 범위에 포함되는 클래스 또는 메소드를 표시합니다.
 * SSO Agent 전체가 TOE 범위에 포함됩니다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToeScope {

    String value() default "";

    String[] sfr() default {};
}

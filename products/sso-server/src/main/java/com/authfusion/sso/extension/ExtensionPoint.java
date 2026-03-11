package com.authfusion.sso.extension;

import java.lang.annotation.*;

/**
 * TOE 코드에서 확장 연결 위치를 표시하는 어노테이션.
 * 이 어노테이션이 붙은 메소드/클래스에서 Extension SPI 호출이 발생할 수 있습니다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtensionPoint {

    /**
     * 확장 지점 설명
     */
    String value() default "";

    /**
     * 연결 가능한 Extension SPI 타입
     */
    Class<?>[] spiTypes() default {};
}

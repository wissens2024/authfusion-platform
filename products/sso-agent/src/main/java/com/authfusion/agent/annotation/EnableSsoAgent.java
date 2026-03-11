package com.authfusion.agent.annotation;

import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.config.SsoAgentAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@ToeScope(value = "SSO Agent 활성화 어노테이션", sfr = {})
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SsoAgentAutoConfiguration.class)
public @interface EnableSsoAgent {
}

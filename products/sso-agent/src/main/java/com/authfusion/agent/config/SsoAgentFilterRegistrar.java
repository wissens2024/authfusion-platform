package com.authfusion.agent.config;

import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.filter.SsoAuthenticationFilter;
import com.authfusion.agent.filter.SsoAuthorizationFilter;
import com.authfusion.agent.filter.SsoLogoutFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ToeScope(value = "SSO Agent 필터 등록", sfr = {})
@Configuration
public class SsoAgentFilterRegistrar {

    @Bean
    public FilterRegistrationBean<SsoLogoutFilter> ssoLogoutFilterRegistration(SsoLogoutFilter filter) {
        FilterRegistrationBean<SsoLogoutFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("ssoLogoutFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SsoAuthenticationFilter> ssoAuthenticationFilterRegistration(
            SsoAuthenticationFilter filter) {
        FilterRegistrationBean<SsoAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        registration.setName("ssoAuthenticationFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SsoAuthorizationFilter> ssoAuthorizationFilterRegistration(
            SsoAuthorizationFilter filter) {
        FilterRegistrationBean<SsoAuthorizationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(3);
        registration.setName("ssoAuthorizationFilter");
        return registration;
    }
}

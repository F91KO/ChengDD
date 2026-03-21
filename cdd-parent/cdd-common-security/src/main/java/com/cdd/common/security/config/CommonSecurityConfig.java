package com.cdd.common.security.config;

import com.cdd.common.security.authentication.JwtAccessDeniedHandler;
import com.cdd.common.security.authentication.JwtAuthenticationEntryPoint;
import com.cdd.common.security.authentication.JwtTokenService;
import com.cdd.common.security.authentication.NimbusJwtTokenService;
import com.cdd.common.security.filter.AuthContextFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration
@EnableConfigurationProperties(CddSecurityProperties.class)
public class CommonSecurityConfig {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenService jwtTokenService(CddSecurityProperties securityProperties) {
        return new NimbusJwtTokenService(securityProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new JwtAccessDeniedHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthContextFilter authContextFilter(JwtTokenService jwtTokenService,
                                               AuthenticationEntryPoint authenticationEntryPoint) {
        return new AuthContextFilter(jwtTokenService, authenticationEntryPoint);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthContextFilter authContextFilter,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler,
                                                   CddSecurityProperties securityProperties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(authContextFilter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(EndpointRequest.to("health", "info")).permitAll();
                    authorize.requestMatchers(securityProperties.getPermitPaths().toArray(String[]::new)).permitAll();
                    if (securityProperties.isPermitAll()) {
                        authorize.anyRequest().permitAll();
                    } else {
                        authorize.anyRequest().authenticated();
                    }
                });
        return http.build();
    }
}

package com.p2p.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // Disable basic auth prompt
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable) // Disable form login
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/**", "/eureka/**", "/fallback/**").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }
}
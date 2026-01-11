package com.project.user_service.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@AllArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final HandlerExceptionResolver handlerExceptionResolver;
        public final HeaderFilterChain headerFilterChain;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
                return httpSecurity
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/v3/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/error",
                                                                "/grpc.**",
                                                                "/Recommendation/**",
                                                                "/recommendation/**",
                                                                "/recommendation.Recommendation/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(headerFilterChain, UsernamePasswordAuthenticationFilter.class)
                                .exceptionHandling(exceptionConfig -> exceptionConfig.accessDeniedHandler(
                                                (request, response, accessDeniedException) -> handlerExceptionResolver
                                                                .resolveException(request, response, null,
                                                                                accessDeniedException)))
                                .build();
        }

}

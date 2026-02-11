package com.weekly.config;

import com.weekly.security.CustomOidcUserService;
import com.weekly.security.SsoSuccessHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name = "app.sso.enabled", havingValue = "true")
@Import(OAuth2ClientAutoConfiguration.class)
public class SsoSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
            CustomOidcUserService oidcUserService,
            SsoSuccessHandler successHandler) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/css/**", "/js/**", "/favicon.ico", "/.well-known/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(o -> o
                .userInfoEndpoint(u -> u.oidcUserService(oidcUserService))
                .successHandler(successHandler))
            .logout(l -> l.logoutSuccessUrl("/"));
        return http.build();
    }
}

package com.fennixs.auth.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(RegistrationProperties registration, SecurityProperties security) {
    public record RegistrationProperties(boolean allow) {}

    public record SecurityProperties(JwtProperties jwt, boolean cookieSecure, List<String> corsAllowedOrigins) {
        public record JwtProperties(String secret, long accessTokenExpirationMs) {}
    }
}

package com.fennixs.auth.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fennixs.auth.config.AppProperties;
import com.fennixs.auth.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupTokenService {
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    private final AtomicReference<String> setupToken = new AtomicReference<>();

    @PostConstruct
    private void init() {
        if (userRepository.count() == 0) {
            String token = UUID.randomUUID().toString();
            setupToken.set(token);
            log.warn("========== NO USERS FOUND - SETUP MODE ACTIVE =============");
            log.warn("Register the first user");
            log.warn("Setup token: {}", token);
            log.warn("===========================================================");
        }
    }

    public enum Grant {
        DENIED,
        OPEN_REGISTRATION,
        SETUP_TOKEN
    }

    public Grant consume(String token) {
        if (appProperties.registration().allow()) return Grant.OPEN_REGISTRATION;

        String current = setupToken.get();
        if (current == null || !StringUtils.hasText(token)) return Grant.DENIED;

        if (!MessageDigest.isEqual(current.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
            return Grant.DENIED;

        return setupToken.compareAndSet(current, null) ? Grant.SETUP_TOKEN : Grant.DENIED;
    }

    public void restore(String token) {
        if (appProperties.registration().allow() || !StringUtils.hasText(token)) return;

        if (setupToken.compareAndSet(null, token)) {
            log.info("Setup token restored after failed registration");
        }
    }
}

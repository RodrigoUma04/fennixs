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

    public boolean tryConsume(String token) {
        if (appProperties.registration().allow()) return true;

        String current = setupToken.get();
        if (current == null || !StringUtils.hasText(token)) return false;

        if (!MessageDigest.isEqual(current.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
            return false;

        return setupToken.compareAndSet(current, null);
    }

    public void restore(String token) {
        if (appProperties.registration().allow() || !StringUtils.hasText(token)) return;

        if (setupToken.compareAndSet(null, token)) {
            log.info("Setup token restored after failed registration");
        }
    }
}

package com.fennixs.auth.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.common.exception.AuthException;
import com.fennixs.auth.common.exception.BusinessException;
import com.fennixs.auth.user.entity.User;
import com.fennixs.auth.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final SetupTokenService setupTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequestDto request) {
        if (!setupTokenService.tryConsume(request.setupToken()))
            throw new AuthException("Invalid or missing setup token", HttpStatus.FORBIDDEN);

        try {
            if (userRepository.existsByEmail(request.email().toLowerCase().trim()))
                throw new BusinessException("A resource with the provided data already exists", HttpStatus.CONFLICT);

            User user = User.builder()
                    .email(request.email())
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .build();

            userRepository.saveAndFlush(user);
            log.info("user_id={} registered", user.getId());
        } catch (RuntimeException e) {
            setupTokenService.restore(request.setupToken());
            throw e;
        }
    }
}

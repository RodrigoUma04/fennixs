package com.fennixs.auth.auth.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.service.SetupTokenService.Grant;
import com.fennixs.auth.common.exception.AuthException;
import com.fennixs.auth.common.exception.BusinessException;
import com.fennixs.auth.user.entity.Role;
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
        Grant grant = setupTokenService.consume(request.setupToken());
        if (grant == Grant.DENIED) throw new AuthException("Invalid or missing setup token", HttpStatus.FORBIDDEN);

        try {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            Role role = grant == Grant.SETUP_TOKEN ? Role.OWNER : Role.USER;

            UUID userId = createUser(email, request.password(), role);

            log.info("user_id={} registered", userId);
        } catch (RuntimeException e) {
            setupTokenService.restore(request.setupToken());
            throw e;
        }
    }

    private UUID createUser(String email, String password, Role role) {
        if (userRepository.existsByEmail(email))
            throw new BusinessException("A resource with the provided data already exists", HttpStatus.CONFLICT);

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();

        userRepository.saveAndFlush(user);

        return user.getId();
    }
}

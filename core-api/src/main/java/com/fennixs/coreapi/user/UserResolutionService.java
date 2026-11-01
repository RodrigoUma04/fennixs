package com.fennixs.coreapi.user;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserResolutionService {
    private final UserRepository repository;
    private final JdbcTemplate jdbc;

    @Transactional
    public User resolve(String issuer, String subject, @Nullable String email) {
        jdbc.queryForMap(
                "SELECT set_config('app.login_iss', ?, true), set_config('app.login_sub', ?, true)", issuer, subject);

        User user =
                repository.findByIssuerAndSubject(issuer, subject).orElseGet(() -> new User(issuer, subject, email));
        user.setEmail(email);
        return repository.save(user);
    }
}

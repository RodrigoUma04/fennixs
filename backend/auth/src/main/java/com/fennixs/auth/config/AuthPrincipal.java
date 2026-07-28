package com.fennixs.auth.config;

import java.util.UUID;

import com.fennixs.auth.user.entity.Role;

public record AuthPrincipal(UUID userId, String email, Role role) {}

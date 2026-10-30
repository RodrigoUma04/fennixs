package com.fennixs.coreapi.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByIssuerAndSubject(String issuer, String subject);
}

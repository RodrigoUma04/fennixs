package com.fennixs.coreapi.security;

import java.util.UUID;

import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import lombok.Getter;

@Getter
public class FennixsOidcUser extends DefaultOidcUser {
    private final UUID userId;

    public FennixsOidcUser(OidcUser user, UUID userId) {
        super(user.getAuthorities(), user.getIdToken(), user.getUserInfo());
        this.userId = userId;
    }
}

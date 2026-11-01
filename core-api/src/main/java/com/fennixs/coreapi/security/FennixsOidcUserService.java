package com.fennixs.coreapi.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.fennixs.coreapi.user.User;
import com.fennixs.coreapi.user.UserResolutionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class FennixsOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final UserResolutionService userResolutionService;

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcIdToken idToken = request.getIdToken();
        User user = userResolutionService.resolve(
                idToken.getClaimAsString(IdTokenClaimNames.ISS), idToken.getSubject(), idToken.getEmail());
        if (!user.isEnabled()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED));
        }
        return new FennixsOidcUser(new DefaultOidcUser(AuthorityUtils.NO_AUTHORITIES, idToken), user.getId());
    }
}

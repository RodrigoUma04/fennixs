package com.fennixs.coreapi.user;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fennixs.coreapi.TestOidcConfiguration;
import com.fennixs.coreapi.TestcontainersConfiguration;
import com.fennixs.coreapi.security.FennixsOidcUser;

@Import({TestcontainersConfiguration.class, TestOidcConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
class CurrentUserTests {
    @Autowired
    MockMvc mockMvc;

    @Test
    void givenSignedInUserWithEmail_whenGettingMe_thenReturnsIdAndEmail() throws Exception {
        // arrange
        UUID id = UUID.randomUUID();

        // act + assert
        mockMvc.perform(get("/api/me").with(oidcLogin().oidcUser(principal(id, "ada@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void givenSignedInUserWithoutEmail_whenGettingMe_thenEmailIsNull() throws Exception {
        // arrange
        UUID id = UUID.randomUUID();

        // act + assert
        mockMvc.perform(get("/api/me").with(oidcLogin().oidcUser(principal(id, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value(nullValue()));
    }

    @Test
    void givenNoSession_whenGettingMe_thenReturns401() throws Exception {
        // act + assert
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    private static FennixsOidcUser principal(UUID id, String email) {
        OidcIdToken.Builder token =
                OidcIdToken.withTokenValue("t").issuer("https://idp.test").subject("sub-1");
        if (email != null) {
            token.claim("email", email);
        }
        return new FennixsOidcUser(new DefaultOidcUser(AuthorityUtils.NO_AUTHORITIES, token.build()), id);
    }
}

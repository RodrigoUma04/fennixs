package com.fennixs.coreapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import com.fennixs.coreapi.TestOidcConfiguration;
import com.fennixs.coreapi.TestcontainersConfiguration;

@Import({TestcontainersConfiguration.class, TestOidcConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {
    @Autowired
    MockMvc mockMvc;

    @Test
    void givenNoSession_whenRequestingApi_thenReturns401WithoutRedirect() throws Exception {
        // act + assert
        mockMvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                        .isNull());
    }

    @Test
    void givenNoSession_whenStartingLogin_thenAuthorizationRequestUsesPkceS256() throws Exception {
        // act
        String location = mockMvc.perform(get("/oauth2/authorization/oidc"))
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        // assert
        var params = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        assertThat(params.getFirst("response_type")).isEqualTo("code");
        assertThat(params.getFirst("code_challenge")).isNotBlank();
        assertThat(params.getFirst("code_challenge_method")).isEqualTo("S256");
    }
}

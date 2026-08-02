package com.fennixs.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fennixs.auth.auth.service.JwtService;
import com.fennixs.auth.user.entity.Role;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterUnitTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@fennixs.com";
    private static final String TOKEN = "signed-jwt";

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenValidAccessTokenCookie_whenDoFilter_thenAuthenticateWithRolePrefixedAuthority() throws Exception {
        // Arrange
        when(jwtService.isValid(TOKEN)).thenReturn(true);
        when(jwtService.extractSubject(TOKEN)).thenReturn(USER_ID.toString());
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(jwtService.extractRole(TOKEN)).thenReturn(Role.OWNER);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieNames.ACCESS_TOKEN, TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(new AuthPrincipal(USER_ID, EMAIL, Role.OWNER));
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OWNER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void givenNoAccessTokenCookie_whenDoFilter_thenContinueChainWithoutAuthenticating() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void givenInvalidAccessTokenCookie_whenDoFilter_thenContinueChainWithoutAuthenticating() throws Exception {
        // Arrange
        when(jwtService.isValid(TOKEN)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieNames.ACCESS_TOKEN, TOKEN));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}

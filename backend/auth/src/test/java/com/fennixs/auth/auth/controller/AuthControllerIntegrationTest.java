package com.fennixs.auth.auth.controller;

import static com.fennixs.auth.config.CookieNames.ACCESS_TOKEN;
import static com.fennixs.auth.config.CookieNames.REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fennixs.auth.TestcontainersConfiguration;
import com.fennixs.auth.auth.dto.LoginRequestDto;
import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.repository.RefreshTokenRepository;
import com.fennixs.auth.auth.service.SetupTokenService;
import com.fennixs.auth.auth.util.LoginRequestDtoObjectMother;
import com.fennixs.auth.auth.util.RegisterRequestDtoObjectMother;
import com.fennixs.auth.auth.util.TestCredentials;
import com.fennixs.auth.user.entity.Role;
import com.fennixs.auth.user.entity.User;
import com.fennixs.auth.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    private static final String EMAIL = TestCredentials.DEFAULT_EMAIL;
    private static final String PASSWORD = TestCredentials.DEFAULT_PASSWORD;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    SetupTokenService setupTokenService;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String setupToken;

    @BeforeEach
    void resetState() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        String freshToken = UUID.randomUUID().toString();
        tokenRef(setupTokenService).set(freshToken);
        setupToken = freshToken;
    }

    // region Register
    @Test
    void givenValidSetupToken_whenRegister_thenReturnCreatedAndPersistUser() throws Exception {
        // Act & Assert
        performRegister(RegisterRequestDtoObjectMother.createRegisterRequestDto(setupToken, EMAIL, PASSWORD))
                .andExpect(status().isCreated());

        assertThat(tokenRef(setupTokenService).get()).isNull();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().getRole()).isEqualTo(Role.OWNER);
    }

    @Test
    void givenInvalidSetupToken_whenRegister_thenReturnForbidden() throws Exception {
        // Act & Assert
        performRegister(RegisterRequestDtoObjectMother.createRegisterRequestDto("invalid-setup-token", EMAIL, PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Invalid or missing setup token"));

        assertThat(userRepository.count()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void givenBlankSetupToken_whenRegister_thenReturnForbidden(String blankToken) throws Exception {
        // Act & Assert
        performRegister(RegisterRequestDtoObjectMother.createRegisterRequestDto(blankToken, EMAIL, PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Invalid or missing setup token"));

        assertThat(userRepository.count()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"notanemail", "no-at-sign", "  invalid@mail.com  "})
    void givenInvalidEmail_whenRegister_thenReturnUnprocessableContent(String invalidEmail) throws Exception {
        // Act & Assert
        performRegister(RegisterRequestDtoObjectMother.createRegisterRequestDto(setupToken, invalidEmail, PASSWORD))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").doesNotExist());

        assertThat(userRepository.count()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "tooshort",
                "waytoolongpasswordthatwillneverbeacceptedbecausenooneshouldeverhaveapasswordthislonghonestlywhowouldeverdothisbutiguessitsbettertotest"
            })
    void givenInvalidPassword_whenRegister_thenReturnUnprocessableContent(String invalidPassword) throws Exception {
        // Act & Assert
        performRegister(RegisterRequestDtoObjectMother.createRegisterRequestDto(setupToken, EMAIL, invalidPassword))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.email").doesNotExist());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void givenAccountWithSameEmailExists_whenRegister_thenReturnConflictAndRestoreToken() throws Exception {
        // Arrange
        persistUser(EMAIL, PASSWORD);

        // Act & Assert
        performRegister(RegisterRequestDtoObjectMother.createRegisterRequestDto(
                        setupToken, EMAIL, "anotherpassword123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A resource with the provided data already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(tokenRef(setupTokenService).get()).isEqualTo(setupToken);
    }

    @Test
    void givenEmailRegistered_whenRegisterSameEmailWithDifferentCasing_thenReturnConflict() throws Exception {
        // Arrange
        performRegister(RegisterRequestDtoObjectMother.createRegisterRequestDto(
                        setupToken, "User@Fennixs.COM", PASSWORD))
                .andExpect(status().isCreated());

        tokenRef(setupTokenService).set(setupToken);

        // Act & Assert
        performRegister(RegisterRequestDtoObjectMother.createRegisterRequestDto(
                        setupToken, "USER@FENNIXS.COM", "anotherpassword123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A resource with the provided data already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
    }
    // endregion

    // region Login
    @Test
    void givenValidCredentials_whenLogin_thenReturnOkAndSetTokenCookies() throws Exception {
        // Arrange
        persistUser(EMAIL, PASSWORD);

        // Act & Assert
        MvcResult result = performLogin(LoginRequestDtoObjectMother.valid())
                .andExpect(status().isOk())
                .andReturn();

        MockCookie accessTokenCookie = (MockCookie) result.getResponse().getCookie(ACCESS_TOKEN);
        assertThat(accessTokenCookie).isNotNull();
        assertThat(accessTokenCookie.getPath()).isEqualTo("/api/");
        assertThat(accessTokenCookie.isHttpOnly()).isTrue();
        assertThat(accessTokenCookie.getSecure()).isFalse();
        assertThat(accessTokenCookie.getSameSite()).isEqualTo("Strict");

        MockCookie refreshTokenCookie = (MockCookie) result.getResponse().getCookie(REFRESH_TOKEN);
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.getPath()).isEqualTo("/auth/");
        assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
        assertThat(refreshTokenCookie.getSecure()).isFalse();
        assertThat(refreshTokenCookie.getSameSite()).isEqualTo("Strict");

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void givenEmailRegisteredLowercase_whenLoginWithDifferentCasing_thenReturnOk() throws Exception {
        // Arrange
        persistUser(EMAIL, PASSWORD);

        // Act & Assert
        performLogin(LoginRequestDtoObjectMother.createLoginRequestDto("USER@FENNIXS.COM", PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void givenWrongPassword_whenLogin_thenReturnUnauthorizedWithGenericMessage() throws Exception {
        // Arrange
        persistUser(EMAIL, PASSWORD);

        // Act & Assert
        performLogin(LoginRequestDtoObjectMother.createLoginRequestDto(EMAIL, "wrongpassword123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void givenUnknownEmail_whenLogin_thenReturnSameGenericUnauthorizedMessageAsWrongPassword() throws Exception {
        // Act & Assert
        performLogin(LoginRequestDtoObjectMother.valid())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"notanemail", "no-at-sign"})
    void givenInvalidEmail_whenLogin_thenReturnUnprocessableContent(String invalidEmail) throws Exception {
        // Act & Assert
        performLogin(LoginRequestDtoObjectMother.createLoginRequestDto(invalidEmail, PASSWORD))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.email").exists());
    }
    // endregion

    // region Refresh
    @Test
    void givenValidRefreshTokenCookie_whenRefresh_thenRotateCookiesAndInvalidateOldToken() throws Exception {
        // Arrange
        String oldRefreshToken = loginAndGetRefreshToken();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_TOKEN, oldRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshToken = result.getResponse().getCookie(REFRESH_TOKEN).getValue();
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_TOKEN, oldRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid refresh token"));
    }

    @Test
    void givenMissingRefreshTokenCookie_whenRefresh_thenReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Refresh token missing"));
    }

    @Test
    void givenInvalidRefreshTokenCookie_whenRefresh_thenReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_TOKEN, "bogus-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid refresh token"));
    }
    // endregion

    // region Logout
    @Test
    void givenValidRefreshTokenCookie_whenLogout_thenClearCookiesAndRevokeToken() throws Exception {
        // Arrange
        String refreshToken = loginAndGetRefreshToken();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/auth/logout").cookie(new Cookie(REFRESH_TOKEN, refreshToken)))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getCookie(ACCESS_TOKEN).getMaxAge()).isZero();
        assertThat(result.getResponse().getCookie(REFRESH_TOKEN).getMaxAge()).isZero();

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_TOKEN, refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenNoRefreshTokenCookie_whenLogout_thenReturnNoContentIdempotently() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/logout")).andExpect(status().isNoContent());
    }
    // endregion

    // region Verify
    @Test
    void givenNoAccessTokenCookie_whenVerify_thenReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/auth/verify")).andExpect(status().isUnauthorized());
    }

    @Test
    void givenValidAccessTokenCookie_whenVerify_thenReturnOkWithUserIdAndRoleHeaders() throws Exception {
        // Arrange
        persistUser(EMAIL, PASSWORD);
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        MvcResult loginResult = performLogin(LoginRequestDtoObjectMother.valid())
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = loginResult.getResponse().getCookie(ACCESS_TOKEN).getValue();

        // Act & Assert
        mockMvc.perform(get("/auth/verify").cookie(new Cookie(ACCESS_TOKEN, accessToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-User-Id", user.getId().toString()))
                .andExpect(header().string("X-User-Role", Role.USER.name()));
    }

    @Test
    void givenTamperedAccessTokenCookie_whenVerify_thenReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/auth/verify").cookie(new Cookie(ACCESS_TOKEN, "not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }
    // endregion

    // region private helper methods
    private ResultActions performRegister(RegisterRequestDto request) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performLogin(LoginRequestDto request) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private String loginAndGetRefreshToken() throws Exception {
        persistUser(EMAIL, PASSWORD);
        MvcResult result = performLogin(LoginRequestDtoObjectMother.valid())
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie(REFRESH_TOKEN).getValue();
    }

    private void persistUser(String email, String rawPassword) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.USER)
                .build();
        userRepository.save(user);
    }

    @SuppressWarnings("unchecked")
    private static AtomicReference<String> tokenRef(SetupTokenService svc) {
        return (AtomicReference<String>) ReflectionTestUtils.getField(svc, "setupToken");
    }
    // endregion
}

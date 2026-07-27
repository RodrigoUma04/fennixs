package com.fennixs.auth.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fennixs.auth.TestcontainersConfiguration;
import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.service.SetupTokenService;
import com.fennixs.auth.auth.util.RegisterRequestDtoObjectMother;
import com.fennixs.auth.user.entity.Role;
import com.fennixs.auth.user.entity.User;
import com.fennixs.auth.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    private static final String EMAIL = RegisterRequestDtoObjectMother.DEFAULT_EMAIL;
    private static final String PASSWORD = RegisterRequestDtoObjectMother.DEFAULT_PASSWORD;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SetupTokenService setupTokenService;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String setupToken;

    @BeforeEach
    void resetState() {
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

    // region private helper methods
    private ResultActions performRegister(RegisterRequestDto request) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
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

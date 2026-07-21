package com.fennixs.auth.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fennixs.auth.TestcontainersConfiguration;
import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.service.AuthService;
import com.fennixs.auth.auth.util.RegisterRequestDtoObjectMother;

/**
 * Covers the unique-constraint race path (a {@code DataIntegrityViolationException} surfacing after
 * {@code existsByEmail} already passed), which the full-stack {@code AuthControllerIntegrationTest}
 * cannot drive deterministically without a mocked service. The valid request body always passes
 * validation so the request reaches the service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthControllerExceptionHandlingIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    void givenServiceHitsUniqueConstraint_whenRegister_thenReturn409WithGenericDetail() throws Exception {
        // Arrange
        doThrow(new DataIntegrityViolationException("users_email_key violation"))
                .when(authService)
                .register(any(RegisterRequestDto.class));

        // Act & Assert
        performRegister()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A resource with the provided data already exists."));
    }

    private ResultActions performRegister() throws Exception {
        RegisterRequestDto request = RegisterRequestDtoObjectMother.valid();
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}

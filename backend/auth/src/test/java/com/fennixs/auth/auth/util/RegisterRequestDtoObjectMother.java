package com.fennixs.auth.auth.util;

import com.fennixs.auth.auth.dto.RegisterRequestDto;

public class RegisterRequestDtoObjectMother {
    public static final String DEFAULT_SETUP_TOKEN = "setup-token";

    private RegisterRequestDtoObjectMother() {
        /* This utility class should not be instantiated */
    }

    public static RegisterRequestDto valid() {
        return createRegisterRequestDto(
                DEFAULT_SETUP_TOKEN, TestCredentials.DEFAULT_EMAIL, TestCredentials.DEFAULT_PASSWORD);
    }

    public static RegisterRequestDto createRegisterRequestDto(String setupToken, String email, String password) {
        return new RegisterRequestDto(setupToken, email, password);
    }
}

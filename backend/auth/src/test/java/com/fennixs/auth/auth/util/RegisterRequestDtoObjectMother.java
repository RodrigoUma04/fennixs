package com.fennixs.auth.auth.util;

import com.fennixs.auth.auth.dto.RegisterRequestDto;

public class RegisterRequestDtoObjectMother {
    public static final String DEFAULT_SETUP_TOKEN = "setup-token";
    public static final String DEFAULT_EMAIL = "user@fennixs.com";
    public static final String DEFAULT_PASSWORD = "verysecurepassword";

    public static RegisterRequestDto valid() {
        return createRegisterRequestDto(DEFAULT_SETUP_TOKEN, DEFAULT_EMAIL, DEFAULT_PASSWORD);
    }

    public static RegisterRequestDto createRegisterRequestDto(String setupToken, String email, String password) {
        return new RegisterRequestDto(setupToken, email, password);
    }
}

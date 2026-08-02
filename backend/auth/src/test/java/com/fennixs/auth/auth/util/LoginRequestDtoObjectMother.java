package com.fennixs.auth.auth.util;

import com.fennixs.auth.auth.dto.LoginRequestDto;

public class LoginRequestDtoObjectMother {
    private LoginRequestDtoObjectMother() {
        /* This utility class should not be instantiated */
    }

    public static LoginRequestDto valid() {
        return createLoginRequestDto(TestCredentials.DEFAULT_EMAIL, TestCredentials.DEFAULT_PASSWORD);
    }

    public static LoginRequestDto createLoginRequestDto(String email, String password) {
        return new LoginRequestDto(email, password);
    }
}

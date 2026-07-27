package com.fennixs.auth.common.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends BusinessException {
    public AuthException(String message, HttpStatus status) {
        super(message, status);
    }
}

package com.fennixs.auth.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fennixs.auth.auth.dto.RegisterRequestDto;
import com.fennixs.auth.auth.service.AuthService;
import com.fennixs.auth.generated.api.AuthApi;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService service;

    @Override
    public ResponseEntity<Void> register(RegisterRequestDto request) {
        service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

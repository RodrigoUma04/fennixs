package com.fennixs.auth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequestDto(
        @JsonProperty("email") @NotBlank @Email String email,
        @JsonProperty("password") @NotBlank @Size(max = 128) String password) {}

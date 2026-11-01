package com.fennixs.coreapi.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import com.fennixs.coreapi.generated.api.UsersApi;
import com.fennixs.coreapi.generated.model.CurrentUserDto;
import com.fennixs.coreapi.security.FennixsOidcUser;

@RestController
class UserController implements UsersApi {

    @Override
    public ResponseEntity<CurrentUserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof FennixsOidcUser principal)) {
            throw new IllegalStateException("No FennixsOidcUser in the security context");
        }
        return ResponseEntity.ok(new CurrentUserDto(principal.getUserId()).email(principal.getEmail()));
    }
}

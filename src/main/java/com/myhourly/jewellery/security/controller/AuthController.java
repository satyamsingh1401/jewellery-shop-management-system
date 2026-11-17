package com.myhourly.jewellery.security.controller;

import com.myhourly.jewellery.security.dto.LoginRequest;
import com.myhourly.jewellery.security.dto.LoginResponse;
import com.myhourly.jewellery.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.substring(7).trim();

        authService.logout(token);

        return ResponseEntity.noContent().build();
    }
}

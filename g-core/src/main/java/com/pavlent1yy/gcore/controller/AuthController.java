package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.dto.*;
import com.pavlent1yy.gcore.dto.records.LoginResponse;
import com.pavlent1yy.gcore.dto.records.UserResponse;
import com.pavlent1yy.gcore.service.AuthService;
import com.pavlent1yy.gcore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public UserResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authService.createRefreshCookie(response.refreshToken()).toString()
                )
                .body(new LoginResponse(
                        response.accessToken(),
                        null
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "gradinator_refresh", required = false)
            String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authService.deleteRefreshCookie().toString()
                )
                .build();
    }


    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getCurrentUser(authentication.getName());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue("gradinator_refresh") String refreshToken
    ) {
        LoginResponse response = authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authService.createRefreshCookie(response.refreshToken()).toString()
                )
                .body(new LoginResponse(
                        response.accessToken(),
                        null
                ));
    }


}

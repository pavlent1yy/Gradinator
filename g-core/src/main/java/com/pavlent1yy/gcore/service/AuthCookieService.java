package com.pavlent1yy.gcore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    @Value("${app.cookies.secure:false}")
    private boolean secure;

    public ResponseCookie accessCookie(String token) {
        return ResponseCookie
                .from("gradinator_access", token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMinutes(15))
                .build();
    }

    public ResponseCookie refreshCookie(String token) {
        return ResponseCookie
                .from("gradinator_refresh", token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(30))
                .build();
    }

    public ResponseCookie clearAccessCookie() {
        return ResponseCookie
                .from("gradinator_access", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie
                .from("gradinator_refresh", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
    }
}

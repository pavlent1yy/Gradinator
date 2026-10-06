package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.dto.records.ChangeGroupRequest;
import com.pavlent1yy.gcore.dto.records.ChangePasswordRequest;
import com.pavlent1yy.gcore.service.UserService;
import com.pavlent1yy.gcore.service.AuthCookieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core/user")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;
    private final AuthCookieService authCookieService;

    @PutMapping("/change-password")
    public void changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
    }

    @PutMapping("/change-group")
    public void changeGroup(Authentication authentication, @RequestBody ChangeGroupRequest request){
        userService.changeGroup(authentication.getName(), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        userService.deleteAccount(authentication.getName());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshCookie().toString())
                .build();
    }

}

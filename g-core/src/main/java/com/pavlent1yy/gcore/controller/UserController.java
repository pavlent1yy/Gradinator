package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.dto.records.ChangePasswordRequest;
import com.pavlent1yy.gcore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/change-password")
    public void changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
    }

}

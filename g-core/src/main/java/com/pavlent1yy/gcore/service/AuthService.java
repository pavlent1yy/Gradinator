package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.UserAlreadyExistsException;
import com.pavlent1yy.gcore.dto.UserResponse;
import com.pavlent1yy.gcore.entity.RegisterRequest;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    @Value("${core.email-verification}")
    public boolean emailVerificationEnabled;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request){

        log.debug("Start user registration: email={}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()){
            log.warn("Registration failed: user already exists, email={}", request.getEmail());
            throw new UserAlreadyExistsException("User already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setGroup(request.getGroup());
        user.setRegisteredAt(OffsetDateTime.now());
        user.setEnabled(false);

        if (emailVerificationEnabled) {
            //TODO
        } else {
            log.debug("Email verification disabled: auto-enable user, email={}", user.getEmail());
            user.setEnabled(true);
        }

        userRepository.save(user);

        log.info("User registered: email={}, enabled={}", user.getEmail(), user.getEnabled());
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

}

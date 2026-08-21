package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.InvalidRefreshTokenException;
import com.pavlent1yy.gcore.customExceptions.UserAlreadyExistsException;
import com.pavlent1yy.gcore.dto.LoginRequest;
import com.pavlent1yy.gcore.dto.LoginResponse;
import com.pavlent1yy.gcore.dto.UserResponse;
import com.pavlent1yy.gcore.dto.RegisterRequest;
import com.pavlent1yy.gcore.entity.RefreshSession;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;

import static java.util.Objects.hash;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    @Value("${core.email-verification}")
    public boolean emailVerificationEnabled;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshSessionRepository refreshSessionRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

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
        return new UserResponse(user.getId(), user.getEmail(), user.getGroup(), user.getRole());
    }

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Пользователь не найден"
                ));
        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateToken(userDetails);

        String refreshToken = refreshTokenService.create(user);

        return new LoginResponse(accessToken, refreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshSession findValidSession(String token) {
        RefreshSession session = refreshSessionRepository
                .findByRefreshTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid refresh token"
                ));

        if (session.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        return session;
    }


    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public LoginResponse refresh(String refreshToken) {

        RefreshSession session = findValidSession(refreshToken);
        User user = session.getUser();

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateToken(userDetails);

        String newRefreshToken =
                refreshTokenService.rotate(session);

        return new LoginResponse(
                accessToken,
                newRefreshToken
        );
    }



}

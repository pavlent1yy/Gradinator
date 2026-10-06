package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.InvalidRefreshTokenException;
import com.pavlent1yy.gcore.customExceptions.UserAlreadyExistsException;
import com.pavlent1yy.gcore.dto.LoginRequest;
import com.pavlent1yy.gcore.dto.records.LoginResponse;
import com.pavlent1yy.gcore.dto.records.UserResponse;
import com.pavlent1yy.gcore.dto.RegisterRequest;
import com.pavlent1yy.gcore.entity.RefreshSession;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import com.pavlent1yy.gcore.service.jwt.JwtRefreshTokenService;
import com.pavlent1yy.gcore.service.jwt.JwtService;
import com.pavlent1yy.gcore.service.jwt.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import com.pavlent1yy.gcore.customExceptions.EmailNotVerifiedException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;

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
    private final ScheduleService scheduleService;
    private final JwtService jwtService;
    private final JwtRefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;

    @Transactional
    public UserResponse register(RegisterRequest request){

        log.debug("Start user registration");

        String normalizedEmail = EmailNormalizer.normalize(request.getEmail());
        PasswordPolicy.validate(request.getPassword());

        if (userRepository.findByEmail(normalizedEmail).isPresent()){
            log.warn("Registration failed: user already exists");
            throw new UserAlreadyExistsException("Аккаунт с такой почтой уже есть. Войди или восстанови пароль");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);

        String group = request.getGroup();
        String department = request.getDepartment();

        if (isEmptyOrNull(group)) {
            user.setDepartment(null);
        } else if (!isEmptyOrNull(group) && isEmptyOrNull(department)) {
            user.setDepartment(scheduleService.getDepartmentsByGroup(request.getGroup()));
        } else {
            user.setDepartment(department);
        }

        user.setGroup(group);
        user.setRegisteredAt(OffsetDateTime.now());
        user.setEnabled(false);

        userRepository.save(user);

        if (emailVerificationEnabled) {
            emailVerificationService.sendVerificationEmail(user);
        } else {
            log.debug("Email verification disabled: auto-enable user");
            user.setEnabled(true);
        }

        userRepository.save(user);

        log.debug("User registered: enabled={}", user.getEnabled());
        return new UserResponse(user.getId(), user.getEmail(), user.getGroup(), user.getDepartment(), user.getRole(), user.getPasswordHash() != null);
    }

    public LoginResponse login(LoginRequest request) {
        String email = EmailNormalizer.normalize(request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (DisabledException e) {
            User disabled = userRepository.findByEmail(email).orElse(null);
            boolean passwordMatches = disabled != null
                    && disabled.getPasswordHash() != null
                    && passwordEncoder.matches(request.getPassword(), disabled.getPasswordHash());
            if (passwordMatches) {
                throw new EmailNotVerifiedException(
                        "Почта ещё не подтверждена. Перейди по ссылке из письма или запроси новое"
                );
            }
            throw new BadCredentialsException("Bad credentials");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));

        return tokenService.createSession(user);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshSession findValidSession(String token) {
        RefreshSession session = refreshSessionRepository
                .findByRefreshTokenHash(hash(token))
                .orElseThrow(() -> new InvalidRefreshTokenException("Сессия не найдена, войди заново"));

        if (session.getRevokedAt() != null) {
            log.warn("Reuse of revoked refresh token, revoking all sessions");
            refreshSessionRepository.deleteAllByUser_Id(session.getUser().getId());
            throw new InvalidRefreshTokenException("Сессия завершена, войди заново");
        }

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidRefreshTokenException("Сессия истекла, войди заново");
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

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
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

    private boolean isEmptyOrNull(String string){
        return !StringUtils.hasLength(string);
    }



}

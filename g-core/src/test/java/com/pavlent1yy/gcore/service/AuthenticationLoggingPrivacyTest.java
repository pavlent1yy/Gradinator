package com.pavlent1yy.gcore.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pavlent1yy.gcore.customExceptions.UserAlreadyExistsException;
import com.pavlent1yy.gcore.dto.RegisterRequest;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import com.pavlent1yy.gcore.service.jwt.JwtRefreshTokenService;
import com.pavlent1yy.gcore.service.jwt.JwtService;
import com.pavlent1yy.gcore.service.jwt.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationLoggingPrivacyTest {
    private static final String EMAIL = "privacy-canary@example.invalid";
    private static final String PASSWORD = "privacy-canary-password";
    private static final String PASSWORD_HASH = "privacy-canary-hash";
    private static final Long USER_ID = 987654321L;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private RefreshSessionRepository refreshSessionRepository;
    @Mock private ScheduleService scheduleService;
    @Mock private JwtService jwtService;
    @Mock private JwtRefreshTokenService refreshTokenService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private TokenService tokenService;
    @InjectMocks private AuthService authService;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void successfulRegistrationKeepsEventsButOmitsPersonalData(boolean verificationEnabled) {
        authService.emailVerificationEnabled = verificationEnabled;
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        try (CapturedLog capture = new CapturedLog(AuthService.class)) {
            authService.register(request());
            if (verificationEnabled) {
                capture.assertMessages("Start user registration", "User registered: enabled=false");
            } else {
                capture.assertMessages("Start user registration",
                        "Email verification disabled: auto-enable user", "User registered: enabled=true");
            }
            capture.assertNoPersonalData();
        }
    }

    @Test
    void duplicateRegistrationOmitsEmailFromWarning() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        try (CapturedLog capture = new CapturedLog(AuthService.class)) {
            assertThatThrownBy(() -> authService.register(request()))
                    .isInstanceOf(UserAlreadyExistsException.class);
            capture.assertMessages("Start user registration", "Registration failed: user already exists");
            capture.assertNoPersonalData();
        }
    }

    @Test
    void successfulLookupOmitsEmailAndUserIdEvenAtDebug() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        try (CapturedLog capture = new CapturedLog(UserDetailsServiceImpl.class)) {
            new UserDetailsServiceImpl(userRepository).loadUserByUsername(EMAIL);
            capture.assertMessages("Attempt to load user for authentication", "User found for authentication");
            capture.assertNoPersonalData();
        }
    }

    @Test
    void failedLookupOmitsEmailFromWarning() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        try (CapturedLog capture = new CapturedLog(UserDetailsServiceImpl.class)) {
            assertThatThrownBy(() -> new UserDetailsServiceImpl(userRepository).loadUserByUsername(EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class);
            capture.assertMessages("Attempt to load user for authentication", "User not found during authentication");
            capture.assertNoPersonalData();
        }
    }

    private static RegisterRequest request() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setConfirmPassword(PASSWORD);
        return request;
    }

    private static User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setPasswordHash(PASSWORD_HASH);
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        return user;
    }

    private static final class CapturedLog implements AutoCloseable {
        private final Logger logger;
        private final Level previousLevel;
        private final boolean previousAdditive;
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

        private CapturedLog(Class<?> type) {
            logger = (Logger) LoggerFactory.getLogger(type);
            previousLevel = logger.getLevel();
            previousAdditive = logger.isAdditive();
            appender.start();
            logger.addAppender(appender);
            logger.setAdditive(false);
            logger.setLevel(Level.DEBUG);
        }

        private void assertMessages(String... messages) {
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(messages);
        }

        private void assertNoPersonalData() {
            for (ILoggingEvent event : appender.list) {
                assertThat(event.getFormattedMessage()).doesNotContain(EMAIL, PASSWORD, PASSWORD_HASH, USER_ID.toString());
                assertThat(Arrays.toString(event.getArgumentArray()))
                        .doesNotContain(EMAIL, PASSWORD, PASSWORD_HASH, USER_ID.toString());
                assertThat(event.getThrowableProxy()).isNull();
            }
        }

        @Override
        public void close() {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
        }
    }
}

package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.config.UserDetailsImpl;
import com.pavlent1yy.gcore.customExceptions.InvalidRefreshTokenException;
import com.pavlent1yy.gcore.customExceptions.UserAlreadyExistsException;
import com.pavlent1yy.gcore.dto.LoginRequest;
import com.pavlent1yy.gcore.dto.RegisterRequest;
import com.pavlent1yy.gcore.dto.records.LoginResponse;
import com.pavlent1yy.gcore.dto.records.UserResponse;
import com.pavlent1yy.gcore.entity.RefreshSession;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import com.pavlent1yy.gcore.service.jwt.JwtRefreshTokenService;
import com.pavlent1yy.gcore.service.jwt.JwtService;
import com.pavlent1yy.gcore.service.jwt.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtRefreshTokenService refreshTokenService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private static RegisterRequest request(String email, String group, String department) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("secret");
        request.setConfirmPassword("secret");
        request.setGroup(group);
        request.setDepartment(department);
        return request;
    }

    private static String hash(String token) throws Exception {
        return Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(userRepository.findByEmail("new@mail.ru")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hash");

        UserResponse response = authService.register(request("  New@Mail.RU ", null, null));

        assertThat(response.email()).isEqualTo("new@mail.ru");
        assertThat(response.role()).isEqualTo(Role.STUDENT);
        assertThat(response.group()).isNull();
        assertThat(response.department()).isNull();
        verify(userRepository, times(2)).save(argThat(u -> "hash".equals(u.getPasswordHash())));
    }

    @Test
    void registerRejectsExistingEmail() {
        when(userRepository.findByEmail("old@mail.ru")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request("OLD@mail.ru", null, null)))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerResolvesDepartmentByGroup() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(scheduleService.getDepartmentsByGroup("ИС1-33")).thenReturn("oit");

        UserResponse response = authService.register(request("a@mail.ru", "ИС1-33", ""));

        assertThat(response.group()).isEqualTo("ИС1-33");
        assertThat(response.department()).isEqualTo("oit");
    }

    @Test
    void registerKeepsExplicitDepartment() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        UserResponse response = authService.register(request("a@mail.ru", "ИС1-33", "ort"));

        assertThat(response.department()).isEqualTo("ort");
        verifyNoInteractions(scheduleService);
    }

    @Test
    void registerIgnoresDepartmentWithoutGroup() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        UserResponse response = authService.register(request("a@mail.ru", "", "ort"));

        assertThat(response.department()).isNull();
    }

    @Test
    void registerEnablesUserWhenVerificationDisabled() {
        authService.emailVerificationEnabled = false;
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        authService.register(request("a@mail.ru", null, null));

        verify(userRepository, atLeastOnce()).save(argThat(u -> Boolean.TRUE.equals(u.getEnabled())));
        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void registerSendsEmailAndKeepsUserDisabledWhenVerificationEnabled() {
        authService.emailVerificationEnabled = true;
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        authService.register(request("a@mail.ru", null, null));

        verify(emailVerificationService).sendVerificationEmail(argThat(u -> Boolean.FALSE.equals(u.getEnabled())));
    }

    @Test
    void loginAuthenticatesAndCreatesSession() {
        User user = new User();
        LoginRequest request = new LoginRequest();
        request.setEmail("a@mail.ru");
        request.setPassword("secret");
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user));
        when(tokenService.createSession(user)).thenReturn(new LoginResponse("access", "refresh"));

        assertThat(authService.login(request)).isEqualTo(new LoginResponse("access", "refresh"));
        verify(authenticationManager).authenticate(argThat(a ->
                a instanceof UsernamePasswordAuthenticationToken
                        && "a@mail.ru".equals(a.getPrincipal())
                        && "secret".equals(a.getCredentials())));
    }

    @Test
    void loginStopsOnBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("a@mail.ru");
        request.setPassword("wrong");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(tokenService);
    }

    @Test
    void loginFailsWhenUserDisappeared() {
        LoginRequest request = new LoginRequest();
        request.setEmail("a@mail.ru");
        request.setPassword("secret");
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void logoutRevokesRefreshToken() {
        authService.logout("refresh");

        verify(refreshTokenService).revoke("refresh");
    }

    @Test
    void findValidSessionLooksUpByHash() throws Exception {
        RefreshSession session = RefreshSession.builder().expiresAt(OffsetDateTime.now().plusDays(1)).build();
        when(refreshSessionRepository.findByRefreshTokenHash(hash("token"))).thenReturn(Optional.of(session));

        assertThat(authService.findValidSession("token")).isSameAs(session);
    }

    @Test
    void findValidSessionRejectsUnknownRevokedAndExpired() throws Exception {
        when(refreshSessionRepository.findByRefreshTokenHash(hash("unknown"))).thenReturn(Optional.empty());
        when(refreshSessionRepository.findByRefreshTokenHash(hash("revoked"))).thenReturn(Optional.of(
                RefreshSession.builder().revokedAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(1)).build()));
        when(refreshSessionRepository.findByRefreshTokenHash(hash("expired"))).thenReturn(Optional.of(
                RefreshSession.builder().expiresAt(OffsetDateTime.now().minusSeconds(1)).build()));

        assertThatThrownBy(() -> authService.findValidSession("unknown")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> authService.findValidSession("revoked")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.findValidSession("expired"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void refreshIssuesNewAccessAndRotatesRefresh() throws Exception {
        User user = new User();
        user.setEmail("a@mail.ru");
        RefreshSession session = RefreshSession.builder().user(user).expiresAt(OffsetDateTime.now().plusDays(1)).build();
        UserDetailsImpl details = new UserDetailsImpl(user);
        when(refreshSessionRepository.findByRefreshTokenHash(hash("old"))).thenReturn(Optional.of(session));
        when(userDetailsService.loadUserByUsername("a@mail.ru")).thenReturn(details);
        when(jwtService.generateToken(details)).thenReturn("access");
        when(refreshTokenService.rotate(session)).thenReturn("new");

        assertThat(authService.refresh("old")).isEqualTo(new LoginResponse("access", "new"));
    }

    @Test
    void refreshCookiesAreStrictAndSecure() {
        ResponseCookie cookie = authService.createRefreshCookie("token");
        ResponseCookie deleted = authService.deleteRefreshCookie();

        assertThat(cookie.getName()).isEqualTo("gradinator_refresh");
        assertThat(cookie.getValue()).isEqualTo("token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
        assertThat(deleted.getValue()).isEmpty();
        assertThat(deleted.getMaxAge()).isEqualTo(Duration.ZERO);
    }
}

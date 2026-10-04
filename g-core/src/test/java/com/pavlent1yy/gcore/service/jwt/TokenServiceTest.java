package com.pavlent1yy.gcore.service.jwt;

import com.pavlent1yy.gcore.config.UserDetailsImpl;
import com.pavlent1yy.gcore.dto.records.LoginResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.UserRepository;
import com.pavlent1yy.gcore.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtRefreshTokenService refreshTokenService;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void createsAccessAndRefreshForManagedUser() {
        User detached = new User();
        detached.setId(5L);
        User managed = new User();
        managed.setId(5L);
        managed.setEmail("user@mail.ru");
        UserDetailsImpl details = new UserDetailsImpl(managed);

        when(userRepository.findById(5L)).thenReturn(Optional.of(managed));
        when(userDetailsService.loadUserByUsername("user@mail.ru")).thenReturn(details);
        when(jwtService.generateToken(details)).thenReturn("access");
        when(refreshTokenService.create(managed)).thenReturn("refresh");

        LoginResponse response = tokenService.createSession(detached);

        assertThat(response).isEqualTo(new LoginResponse("access", "refresh"));
        verify(refreshTokenService).create(managed);
    }

    @Test
    void failsForMissingUser() {
        User user = new User();
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.createSession(user)).isInstanceOf(UsernameNotFoundException.class);
        verifyNoInteractions(jwtService, refreshTokenService);
    }
}

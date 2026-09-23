package com.pavlent1yy.gcore.service.jwt;

import com.pavlent1yy.gcore.dto.records.LoginResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final JwtRefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponse createSession(User user) {

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);

        String refreshToken = refreshTokenService.create(user);

        return new LoginResponse(accessToken, refreshToken);
    }

}

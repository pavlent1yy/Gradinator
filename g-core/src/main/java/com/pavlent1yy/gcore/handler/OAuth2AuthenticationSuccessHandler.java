package com.pavlent1yy.gcore.handler;

import com.pavlent1yy.gcore.dto.records.LoginResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.service.oauth.OAuthAccountService;
import com.pavlent1yy.gcore.service.jwt.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthAccountService oauthAccountService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken =
                (OAuth2AuthenticationToken) authentication;

        OAuth2User oauthUser =
                oauthToken.getPrincipal();

        String registrationId =
                oauthToken.getAuthorizedClientRegistrationId();

        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(
                        registrationId,
                        oauthToken.getName()
                );

        String accessToken = client.getAccessToken().getTokenValue();

        User user = oauthAccountService.getOrCreateUser(oauthUser, registrationId, accessToken);

        LoginResponse loginResponse = tokenService.createSession(user);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), loginResponse);
    }
}

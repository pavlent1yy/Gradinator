package com.pavlent1yy.gcore.handler;

import com.pavlent1yy.gcore.dto.records.LoginResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.service.AuthCookieService;
import com.pavlent1yy.gcore.service.oauth.OAuthAccountService;
import com.pavlent1yy.gcore.service.jwt.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthAccountService oauthAccountService;
    private final TokenService tokenService;
    private final AuthCookieService authCookieService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(registrationId, oauthToken.getName());

        String accessToken = client.getAccessToken().getTokenValue();
        User user = oauthAccountService.getOrCreateUser(oauthUser, registrationId, accessToken);
        LoginResponse tokens = tokenService.createSession(user);

        response.addHeader(HttpHeaders.SET_COOKIE, authCookieService.accessCookie(tokens.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieService.refreshCookie(tokens.refreshToken()).toString());
        response.sendRedirect(frontendUrl + "/profile");
    }
}

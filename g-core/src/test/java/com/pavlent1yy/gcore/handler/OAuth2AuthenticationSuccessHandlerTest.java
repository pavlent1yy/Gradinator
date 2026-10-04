package com.pavlent1yy.gcore.handler;

import com.pavlent1yy.gcore.dto.records.LoginResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.service.AuthCookieService;
import com.pavlent1yy.gcore.service.jwt.TokenService;
import com.pavlent1yy.gcore.service.oauth.OAuthAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private OAuthAccountService oauthAccountService;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @Mock
    private OAuth2AuthenticationToken authentication;

    @Mock
    private OAuth2User oauthUser;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OAuth2AuthorizedClient client;

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://front");

        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(authentication.getName()).thenReturn("g-1");
        when(authorizedClientService.loadAuthorizedClient("google", "g-1")).thenReturn(client);
        when(client.getAccessToken().getTokenValue()).thenReturn("token");
    }

    private void stubSession() {
        when(tokenService.createSession(any())).thenReturn(new LoginResponse("access", "refresh"));
        when(authCookieService.accessCookie(anyString())).thenReturn(ResponseCookie.from("a", "access").build());
        when(authCookieService.refreshCookie(anyString())).thenReturn(ResponseCookie.from("r", "refresh").build());
    }

    @Test
    void redirectsToProfileOnSuccess() throws Exception {
        User user = new User();
        when(oauthAccountService.getOrCreateUser(oauthUser, "google", "token")).thenReturn(user);
        stubSession();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://front/profile");
        verify(tokenService).createSession(user);
    }

    @Test
    void retriesOnceWhenConcurrentLoginCreatedUser() throws Exception {
        User user = new User();
        when(oauthAccountService.getOrCreateUser(oauthUser, "google", "token"))
                .thenThrow(new DataIntegrityViolationException("uk_oauth_provider_user"))
                .thenReturn(user);
        stubSession();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://front/profile");
        verify(oauthAccountService, times(2)).getOrCreateUser(oauthUser, "google", "token");
    }

    @Test
    void redirectsToLoginWithErrorWhenUserCannotBeResolved() throws Exception {
        when(oauthAccountService.getOrCreateUser(oauthUser, "google", "token"))
                .thenThrow(new IllegalStateException("GOOGLE не предоставил обязательные данные пользователя"));

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://front/login?error=oauth");
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        verifyNoInteractions(tokenService);
    }
}

package com.pavlent1yy.gcore.service.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProviderOAuth2UserServiceTest {

    private ProviderOAuth2UserService service;
    private RestOperations restOperations;

    @BeforeEach
    void setUp() {
        service = new ProviderOAuth2UserService();
        restOperations = mock(RestOperations.class);
        service.setRestOperations(restOperations);
    }

    private static OAuth2UserRequest request(String registrationId, String userInfoUri, String nameAttribute) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-" + registrationId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://provider.test/authorize")
                .tokenUri("https://provider.test/token")
                .userInfoUri(userInfoUri)
                .userNameAttributeName(nameAttribute)
                .build();

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-123", Instant.now(), Instant.now().plusSeconds(60));

        return new OAuth2UserRequest(registration, token);
    }

    @SuppressWarnings("unchecked")
    private void respondWith(Map<String, Object> body) {
        when(restOperations.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(body));
    }

    @SuppressWarnings("unchecked")
    private RequestEntity<?> sentRequest() {
        ArgumentCaptor<RequestEntity<?>> captor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restOperations).exchange(captor.capture(), any(ParameterizedTypeReference.class));
        return captor.getValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void vkPostsFormAndUnwrapsUser() {
        respondWith(Map.of("user", Map.of("user_id", "777", "email", "vk@mail.ru")));

        OAuth2User user = service.loadUser(request("vk", "https://id.vk.com/oauth2/user_info", "user_id"));

        assertThat(user.getName()).isEqualTo("777");
        assertThat((String) user.getAttribute("email")).isEqualTo("vk@mail.ru");

        RequestEntity<?> sent = sentRequest();
        assertThat(sent.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(sent.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = (MultiValueMap<String, String>) sent.getBody();
        assertThat(form.getFirst("client_id")).isEqualTo("client-vk");
        assertThat(form.getFirst("access_token")).isEqualTo("access-123");
    }

    @Test
    void vkWithoutUserInResponseFails() {
        respondWith(Map.of("error", "invalid_token"));

        assertThatThrownBy(() -> service.loadUser(request("vk", "https://id.vk.com/oauth2/user_info", "user_id")))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void yandexUsesOAuthAuthorizationHeader() {
        respondWith(Map.of("id", "42", "default_email", "ya@yandex.ru"));

        OAuth2User user = service.loadUser(request("yandex", "https://login.yandex.ru/info?format=json", "id"));

        assertThat(user.getName()).isEqualTo("42");
        RequestEntity<?> sent = sentRequest();
        assertThat(sent.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(sent.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("OAuth access-123");
        assertThat(sent.getUrl().toString()).isEqualTo("https://login.yandex.ru/info?format=json");
    }

    @Test
    void otherProvidersUseDefaultBearerRequest() {
        respondWith(Map.of("id", 1, "login", "octocat"));

        OAuth2User user = service.loadUser(request("github", "https://api.github.com/user", "id"));

        assertThat(user.getName()).isEqualTo("1");
        assertThat(sentRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer access-123");
    }
}

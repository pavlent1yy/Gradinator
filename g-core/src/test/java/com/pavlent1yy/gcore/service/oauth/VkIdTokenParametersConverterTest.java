package com.pavlent1yy.gcore.service.oauth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class VkIdTokenParametersConverterTest {

    private final VkIdTokenParametersConverter converter = new VkIdTokenParametersConverter();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static OAuth2AuthorizationCodeGrantRequest grantRequest(String registrationId) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://app.test/login/oauth2/code/" + registrationId)
                .authorizationUri("https://provider.test/authorize")
                .tokenUri("https://provider.test/token")
                .build();

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://provider.test/authorize")
                .clientId("client")
                .redirectUri(registration.getRedirectUri())
                .state("state-1")
                .build();

        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code-1")
                .redirectUri(registration.getRedirectUri())
                .state("state-1")
                .build();

        return new OAuth2AuthorizationCodeGrantRequest(
                registration, new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));
    }

    private static void callbackWith(String deviceId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/vk");
        if (deviceId != null) request.setParameter("device_id", deviceId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void addsDeviceIdAndStateForVk() {
        callbackWith("device-9");

        MultiValueMap<String, String> parameters = converter.convert(grantRequest("vk"));

        assertThat(parameters.getFirst("device_id")).isEqualTo("device-9");
        assertThat(parameters.getFirst("state")).isEqualTo("state-1");
    }

    @Test
    void skipsDeviceIdWhenCallbackHasNone() {
        callbackWith(null);

        MultiValueMap<String, String> parameters = converter.convert(grantRequest("vk"));

        assertThat(parameters).doesNotContainKey("device_id");
        assertThat(parameters.getFirst("state")).isEqualTo("state-1");
    }

    @Test
    void addsNothingForOtherProviders() {
        callbackWith("device-9");

        assertThat(converter.convert(grantRequest("google"))).isEmpty();
    }
}

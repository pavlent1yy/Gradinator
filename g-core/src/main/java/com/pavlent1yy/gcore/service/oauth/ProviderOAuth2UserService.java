package com.pavlent1yy.gcore.service.oauth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Map;

@Service
public class ProviderOAuth2UserService extends DefaultOAuth2UserService {

    private static final String VK = "vk";
    private static final String YANDEX = "yandex";

    public ProviderOAuth2UserService() {
        OAuth2UserRequestEntityConverter defaultConverter = new OAuth2UserRequestEntityConverter();

        setRequestEntityConverter(request -> switch (registrationId(request)) {
            case VK -> vkUserInfoRequest(request);
            case YANDEX -> yandexUserInfoRequest(request);
            default -> defaultConverter.convert(request);
        });

        setAttributesConverter(request -> VK.equals(registrationId(request))
                ? ProviderOAuth2UserService::unwrapVkUser
                : attributes -> attributes);
    }

    private static String registrationId(OAuth2UserRequest request) {
        return request.getClientRegistration().getRegistrationId();
    }

    private static URI userInfoUri(OAuth2UserRequest request) {
        return URI.create(
                request.getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUri()
        );
    }

    private static RequestEntity<?> vkUserInfoRequest(OAuth2UserRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", request.getClientRegistration().getClientId());
        form.add("access_token", request.getAccessToken().getTokenValue());

        return RequestEntity.post(userInfoUri(request))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form);
    }

    private static RequestEntity<?> yandexUserInfoRequest(OAuth2UserRequest request) {
        return RequestEntity.get(userInfoUri(request))
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + request.getAccessToken().getTokenValue())
                .accept(MediaType.APPLICATION_JSON)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapVkUser(Map<String, Object> attributes) {
        if (attributes.get("user") instanceof Map<?, ?> user) {
            return (Map<String, Object>) user;
        }

        throw new OAuth2AuthenticationException(
                new OAuth2Error(
                        "invalid_user_info_response",
                        "VK ID не вернул данные пользователя: " + attributes.get("error"),
                        null
                )
        );
    }
}

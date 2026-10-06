package com.pavlent1yy.gcore.service.oauth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class VkIdTokenParametersConverter
        implements Converter<OAuth2AuthorizationCodeGrantRequest, MultiValueMap<String, String>> {

    @Override
    public MultiValueMap<String, String> convert(OAuth2AuthorizationCodeGrantRequest grantRequest) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        if (!"vk".equals(grantRequest.getClientRegistration().getRegistrationId())) {
            return parameters;
        }

        parameters.add("state", grantRequest.getAuthorizationExchange().getAuthorizationResponse().getState());

        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String deviceId = attributes.getRequest().getParameter("device_id");
            if (deviceId != null) {
                parameters.add("device_id", deviceId);
            }
        }

        return parameters;
    }
}

package com.pavlent1yy.gcore.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private static final String PROXY = "172.18.0.5";

    private RateLimitFilter filter;
    private FilterChain chain;

    private static RateLimitProperties.Limit limit(long capacity) {
        RateLimitProperties.Limit limit = new RateLimitProperties.Limit();
        limit.setCapacity(capacity);
        limit.setRefillMinutes(1);
        return limit;
    }

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAuth(limit(2));
        properties.setEmail(limit(1));
        properties.setGeneral(limit(3));
        filter = new RateLimitFilter(properties);
        chain = mock(FilterChain.class);
    }

    private MockHttpServletResponse call(String uri, String remoteAddr, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private MockHttpServletResponse call(String uri) throws Exception {
        return call(uri, "203.0.113.1", null);
    }

    @Test
    void rejectsLoginOverLimitWith429() throws Exception {
        assertThat(call("/core/auth/login").getStatus()).isEqualTo(200);
        assertThat(call("/core/auth/login").getStatus()).isEqualTo(200);
        MockHttpServletResponse rejected = call("/core/auth/login");

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(rejected.getHeader("Retry-After"))).isBetween(1L, 60L);
        assertThat(rejected.getContentType()).startsWith("application/json");
        assertThat(rejected.getContentAsString()).contains("Слишком много запросов");
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void loginAndRegisterShareAuthBucket() throws Exception {
        call("/core/auth/login");
        call("/core/auth/register");

        assertThat(call("/core/auth/register").getStatus()).isEqualTo(429);
    }

    @Test
    void resendVerificationHasOwnStrictLimit() throws Exception {
        assertThat(call("/core/auth/resend-verification").getStatus()).isEqualTo(200);
        assertThat(call("/core/auth/resend-verification").getStatus()).isEqualTo(429);

        assertThat(call("/core/auth/login").getStatus()).isEqualTo(200);
    }

    @Test
    void otherPathsUseGeneralLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("/core/schedule").getStatus()).isEqualTo(200);
        }
        assertThat(call("/core/auth/refresh").getStatus()).isEqualTo(429);
        assertThat(call("/core/auth/login").getStatus()).isEqualTo(200);
    }

    @Test
    void usesForwardedClientIpBehindInternalProxy() throws Exception {
        call("/core/auth/resend-verification", PROXY, "198.51.100.7");

        assertThat(call("/core/auth/resend-verification", PROXY, "198.51.100.8").getStatus()).isEqualTo(200);
        assertThat(call("/core/auth/resend-verification", PROXY, "198.51.100.7").getStatus()).isEqualTo(429);
    }

    @Test
    void ignoresSpoofedForwardedEntriesBeforeRealClient() throws Exception {
        call("/core/auth/resend-verification", PROXY, "198.51.100.7");

        MockHttpServletResponse spoofed = call(
                "/core/auth/resend-verification", PROXY, "1.2.3.4, 198.51.100.7, 172.17.0.1"
        );

        assertThat(spoofed.getStatus()).isEqualTo(429);
    }

    @Test
    void ignoresForwardedHeaderFromExternalAddress() throws Exception {
        call("/core/auth/resend-verification", "203.0.113.9", "198.51.100.1");

        assertThat(call("/core/auth/resend-verification", "203.0.113.9", "198.51.100.2").getStatus()).isEqualTo(429);
    }

    @Test
    void fallsBackToRemoteAddrWithoutForwardedHeader() throws Exception {
        call("/core/auth/resend-verification", PROXY, null);

        assertThat(call("/core/auth/resend-verification", PROXY, "  ").getStatus()).isEqualTo(429);
    }
}

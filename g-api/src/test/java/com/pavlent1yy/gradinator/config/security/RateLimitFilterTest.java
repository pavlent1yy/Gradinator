package com.pavlent1yy.gradinator.config.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

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
        properties.setSchedule(limit(3));
        properties.setScheduleAll(limit(1));
        properties.setUser(limit(2));
        properties.setAdmin(limit(1));
        filter = new RateLimitFilter(properties);
        chain = mock(FilterChain.class);
    }

    private MockHttpServletResponse call(String uri, String ip, String group) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(ip);
        if (group != null) request.setParameter("group", group);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private MockHttpServletResponse call(String uri) throws Exception {
        return call(uri, "10.0.0.1", null);
    }

    @Test
    void passesRequestsWithinLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("/api/schedule", "10.0.0.1", "ИС1-33").getStatus()).isEqualTo(200);
        }
        verify(chain, times(3)).doFilter(any(), any());
    }

    @Test
    void rejectsRequestsOverLimitWith429() throws Exception {
        call("/api/admin/snapshots");
        MockHttpServletResponse rejected = call("/api/admin/snapshots");

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isNotNull();
        assertThat(Long.parseLong(rejected.getHeader("Retry-After"))).isBetween(1L, 60L);
        assertThat(rejected.getContentType()).startsWith("application/json");
        assertThat(rejected.getContentAsString()).contains("Too many requests");
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void scheduleForAllGroupsHasOwnStricterLimit() throws Exception {
        assertThat(call("/api/schedule").getStatus()).isEqualTo(200);
        assertThat(call("/api/schedule").getStatus()).isEqualTo(429);

        assertThat(call("/api/schedule", "10.0.0.1", "ИС1-33").getStatus()).isEqualTo(200);
    }

    @Test
    void scheduleSubpathsUseGroupScheduleLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("/api/schedule/today").getStatus()).isEqualTo(200);
        }
        assertThat(call("/api/schedule/today").getStatus()).isEqualTo(429);
    }

    @Test
    void differentIpsHaveSeparateBuckets() throws Exception {
        call("/api/admin/heartbeat/latest", "10.0.0.1", null);

        assertThat(call("/api/admin/heartbeat/latest", "10.0.0.2", null).getStatus()).isEqualTo(200);
        assertThat(call("/api/admin/heartbeat/latest", "10.0.0.1", null).getStatus()).isEqualTo(429);
    }

    @Test
    void categoriesHaveSeparateBuckets() throws Exception {
        call("/api/admin/snapshots");
        assertThat(call("/api/admin/snapshots").getStatus()).isEqualTo(429);

        assertThat(call("/api/user/me").getStatus()).isEqualTo(200);
        assertThat(call("/api/user/me").getStatus()).isEqualTo(200);
        assertThat(call("/api/user/me").getStatus()).isEqualTo(429);
    }

    @Test
    void otherPathsUseScheduleLimitInOwnBucket() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("/api/groups").getStatus()).isEqualTo(200);
        }
        assertThat(call("/api/schedulex").getStatus()).isEqualTo(429);
        assertThat(call("/api/schedule/today").getStatus()).isEqualTo(200);
    }
}

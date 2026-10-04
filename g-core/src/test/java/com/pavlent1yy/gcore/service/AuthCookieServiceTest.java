package com.pavlent1yy.gcore.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    private static AuthCookieService service(boolean secure) {
        AuthCookieService service = new AuthCookieService();
        ReflectionTestUtils.setField(service, "secure", secure);
        return service;
    }

    private static void assertCommon(ResponseCookie cookie, boolean secure) {
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isEqualTo(secure);
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }

    @Test
    void accessCookieLivesFifteenMinutes() {
        ResponseCookie cookie = service(false).accessCookie("access");

        assertThat(cookie.getName()).isEqualTo("gradinator_access");
        assertThat(cookie.getValue()).isEqualTo("access");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(15));
        assertCommon(cookie, false);
    }

    @Test
    void refreshCookieLivesThirtyDays() {
        ResponseCookie cookie = service(true).refreshCookie("refresh");

        assertThat(cookie.getName()).isEqualTo("gradinator_refresh");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
        assertCommon(cookie, true);
    }

    @Test
    void clearCookiesExpireImmediately() {
        AuthCookieService service = service(true);

        for (ResponseCookie cookie : new ResponseCookie[]{service.clearAccessCookie(), service.clearRefreshCookie()}) {
            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
            assertCommon(cookie, true);
        }
        assertThat(service.clearAccessCookie().getName()).isEqualTo("gradinator_access");
        assertThat(service.clearRefreshCookie().getName()).isEqualTo("gradinator_refresh");
    }
}

package com.pavlent1yy.gcore.service.jwt;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    private final UserDetails user = new User("user@mail.ru", "hash",
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

    private MockHttpServletRequest request;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest("GET", "/core/auth/me");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void validToken(String token) {
        when(jwtService.extractUsername(token)).thenReturn("user@mail.ru");
        when(userDetailsService.loadUserByUsername("user@mail.ru")).thenReturn(user);
        when(jwtService.isTokenValid(token, user)).thenReturn(true);
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void authenticatesByBearerHeader() throws Exception {
        validToken("abc");
        request.addHeader("Authorization", "Bearer abc");

        filter.doFilter(request, response, chain);

        assertThat(authentication().getPrincipal()).isSameAs(user);
        assertThat(authentication().getAuthorities()).extracting(Object::toString).containsExactly("ROLE_STUDENT");
        verify(chain).doFilter(request, response);
    }

    @Test
    void authenticatesByAccessCookie() throws Exception {
        validToken("from-cookie");
        request.setCookies(new Cookie("other", "x"), new Cookie("gradinator_access", "from-cookie"));

        filter.doFilter(request, response, chain);

        assertThat(authentication().getName()).isEqualTo("user@mail.ru");
    }

    @Test
    void headerWinsOverCookie() throws Exception {
        validToken("from-header");
        request.addHeader("Authorization", "Bearer from-header");
        request.setCookies(new Cookie("gradinator_access", "from-cookie"));

        filter.doFilter(request, response, chain);

        verify(jwtService).extractUsername("from-header");
        verify(jwtService, never()).extractUsername("from-cookie");
    }

    @Test
    void passesThroughWithoutToken() throws Exception {
        request.addHeader("Authorization", "Basic abc");

        filter.doFilter(request, response, chain);

        assertThat(authentication()).isNull();
        verifyNoInteractions(jwtService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void brokenTokenIsIgnored() throws Exception {
        when(jwtService.extractUsername("bad")).thenThrow(new MalformedJwtException("bad"));
        request.addHeader("Authorization", "Bearer bad");

        filter.doFilter(request, response, chain);

        assertThat(authentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenNotValidForUserIsIgnored() throws Exception {
        when(jwtService.extractUsername("abc")).thenReturn("user@mail.ru");
        when(userDetailsService.loadUserByUsername("user@mail.ru")).thenReturn(user);
        when(jwtService.isTokenValid("abc", user)).thenReturn(false);
        request.addHeader("Authorization", "Bearer abc");

        filter.doFilter(request, response, chain);

        assertThat(authentication()).isNull();
    }

    @Test
    void existingAuthenticationIsKept() throws Exception {
        TestingAuthenticationToken existing = new TestingAuthenticationToken("admin", null);
        SecurityContextHolder.getContext().setAuthentication(existing);
        when(jwtService.extractUsername("abc")).thenReturn("user@mail.ru");
        request.addHeader("Authorization", "Bearer abc");

        filter.doFilter(request, response, chain);

        assertThat(authentication()).isSameAs(existing);
        verifyNoInteractions(userDetailsService);
    }
}

package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.config.UserDetailsImpl;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository);

    private static User user(Role role, boolean enabled, String passwordHash) {
        User user = new User();
        user.setEmail("a@mail.ru");
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPasswordHash(passwordHash);
        return user;
    }

    @Test
    void loadsUserWithRoleAuthority() {
        User user = user(Role.ADMIN, true, "hash");
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("a@mail.ru");

        assertThat(details).isInstanceOf(UserDetailsImpl.class);
        assertThat(((UserDetailsImpl) details).getUser()).isSameAs(user);
        assertThat(details.getUsername()).isEqualTo("a@mail.ru");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void disabledAndOAuthUsersAreReflected() {
        User user = user(Role.STUDENT, false, null);
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("a@mail.ru");

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.getPassword()).isNull();
    }

    @Test
    void unknownUserGivesGenericMessage() {
        when(userRepository.findByEmail("x@mail.ru")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("x@mail.ru"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");
    }
}

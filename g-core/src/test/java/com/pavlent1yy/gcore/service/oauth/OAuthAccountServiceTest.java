package com.pavlent1yy.gcore.service.oauth;

import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.entity.UserOAuthAccount;
import com.pavlent1yy.gcore.enums.OAuthProvider;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.UserOAuthAccountRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

    @Mock
    private UserOAuthAccountRepository oauthAccountRepository;

    @Mock
    private GitHubOAuthService githubOAuthService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuthAccountService oauthAccountService;

    private OAuth2User googleUser(String sub, String email) {
        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("sub")).thenReturn(sub);
        when(oauthUser.getAttribute("email")).thenReturn(email);
        return oauthUser;
    }

    @Test
    void createsUserAndAccountForNewEmail() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@mail.ru")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = oauthAccountService.getOrCreateUser(
                googleUser("g-1", "  New@Mail.RU "), "google", "token");

        assertThat(user.getEmail()).isEqualTo("new@mail.ru");
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getEnabled()).isTrue();
        assertThat(user.getRegisteredAt()).isNotNull();

        ArgumentCaptor<UserOAuthAccount> account = ArgumentCaptor.forClass(UserOAuthAccount.class);
        verify(oauthAccountRepository).save(account.capture());
        assertThat(account.getValue().getUser()).isSameAs(user);
        assertThat(account.getValue().getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(account.getValue().getProviderUserId()).isEqualTo("g-1");
    }

    @Test
    void linksAccountToExistingPasswordUser() {
        User existing = new User();
        existing.setEmail("old@mail.ru");
        existing.setPasswordHash("hash");

        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-2"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("old@mail.ru")).thenReturn(Optional.of(existing));

        User user = oauthAccountService.getOrCreateUser(
                googleUser("g-2", "old@mail.ru"), "google", "token");

        assertThat(user).isSameAs(existing);
        verify(userRepository, never()).save(any());
        verify(oauthAccountRepository).save(any(UserOAuthAccount.class));
    }

    @Test
    void returnsLinkedUserOnRepeatedLogin() {
        User linked = new User();
        UserOAuthAccount account = new UserOAuthAccount();
        account.setUser(linked);

        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-3"))
                .thenReturn(Optional.of(account));

        User user = oauthAccountService.getOrCreateUser(
                googleUser("g-3", "linked@mail.ru"), "google", "token");

        assertThat(user).isSameAs(linked);
        verifyNoInteractions(userRepository);
        verify(oauthAccountRepository, never()).save(any());
    }

    @Test
    void githubUsesNumericIdAndPrimaryEmailFromApi() {
        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("id")).thenReturn(12345);
        when(githubOAuthService.getEmail("gh-token")).thenReturn("dev@mail.ru");
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GITHUB, "12345"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("dev@mail.ru")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = oauthAccountService.getOrCreateUser(oauthUser, "github", "gh-token");

        assertThat(user.getEmail()).isEqualTo("dev@mail.ru");
        ArgumentCaptor<UserOAuthAccount> account = ArgumentCaptor.forClass(UserOAuthAccount.class);
        verify(oauthAccountRepository).save(account.capture());
        assertThat(account.getValue().getProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(account.getValue().getProviderUserId()).isEqualTo("12345");
    }

    @Test
    void vkUsesUserIdAttribute() {
        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttribute("user_id")).thenReturn(777L);
        when(oauthUser.getAttribute("email")).thenReturn("vk@mail.ru");
        User linked = new User();
        UserOAuthAccount account = new UserOAuthAccount();
        account.setUser(linked);
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.VK, "777"))
                .thenReturn(Optional.of(account));

        assertThat(oauthAccountService.getOrCreateUser(oauthUser, "vk", "token")).isSameAs(linked);
    }

    @Test
    void doesNotDuplicateAlreadyExistingAccountLink() {
        User existing = new User();
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-5"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("old@mail.ru")).thenReturn(Optional.of(existing));
        when(oauthAccountRepository.existsByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-5")).thenReturn(true);

        assertThat(oauthAccountService.getOrCreateUser(googleUser("g-5", "old@mail.ru"), "google", "token"))
                .isSameAs(existing);
        verify(oauthAccountRepository, never()).save(any());
    }

    @Test
    void rejectsLiteralNullEmailAndMissingId() {
        assertThatThrownBy(() -> oauthAccountService.getOrCreateUser(googleUser("g-6", "null"), "google", "token"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> oauthAccountService.getOrCreateUser(googleUser(null, "a@mail.ru"), "google", "token"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsUnknownProvider() {
        assertThatThrownBy(() -> oauthAccountService.getOrCreateUser(mock(OAuth2User.class), "facebook", "token"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userRepository, oauthAccountRepository);
    }

    @Test
    void rejectsProviderWithoutEmail() {
        OAuth2User oauthUser = googleUser("g-4", null);

        assertThatThrownBy(() -> oauthAccountService.getOrCreateUser(oauthUser, "google", "token"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(userRepository);
    }
}

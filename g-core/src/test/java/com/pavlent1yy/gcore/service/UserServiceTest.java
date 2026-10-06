package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.GroupNotFoundException;
import com.pavlent1yy.gcore.customExceptions.PasswordIsIncorrect;
import com.pavlent1yy.gcore.dto.records.ChangeGroupRequest;
import com.pavlent1yy.gcore.dto.records.ChangePasswordRequest;
import com.pavlent1yy.gcore.dto.records.UserResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.AbsenceRepository;
import com.pavlent1yy.gcore.repository.EmailVerificationTokenRepository;
import com.pavlent1yy.gcore.repository.PasswordResetTokenRepository;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserOAuthAccountRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import org.mockito.InOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AbsenceRepository absenceRepository;

    @Mock
    private UserOAuthAccountRepository oauthAccountRepository;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private UserService userService;

    private User user(String passwordHash) {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setPasswordHash(passwordHash);
        when(userRepository.findByEmail("user@mail.ru")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void changesGroupAndDepartment() {
        User user = user("hash");
        when(scheduleService.getAllGroups()).thenReturn(List.of("ИС1-33", "СА1-21"));
        when(scheduleService.getDepartmentsByGroup("ИС1-33")).thenReturn("oit");

        userService.changeGroup("user@mail.ru", new ChangeGroupRequest("ИС1-33"));

        assertThat(user.getGroup()).isEqualTo("ИС1-33");
        assertThat(user.getDepartment()).isEqualTo("oit");
        verify(userRepository).save(user);
    }

    @Test
    void rejectsUnknownGroup() {
        user("hash");
        when(scheduleService.getAllGroups()).thenReturn(List.of("ИС1-33"));

        assertThatThrownBy(() -> userService.changeGroup("user@mail.ru", new ChangeGroupRequest("XX9-99")))
                .isInstanceOf(GroupNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changesPassword() {
        User user = user("old-hash");
        user.setId(4L);
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        userService.changePassword("user@mail.ru", new ChangePasswordRequest("old", "new-password"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        verify(refreshSessionRepository).deleteAllByUser_Id(4L);
    }

    @Test
    void returnsCurrentUser() {
        User user = user("hash");
        user.setId(3L);
        user.setGroup("ИС1-33");
        user.setDepartment("oit");
        user.setRole(Role.STUDENT);

        assertThat(userService.getCurrentUser("user@mail.ru")).isEqualTo(new UserResponse(
                3L, "user@mail.ru", "ИС1-33", "oit", Role.STUDENT, true));
    }

    @Test
    void unknownUserIsRejected() {
        when(userRepository.findByEmail("x@mail.ru")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("x@mail.ru"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsWrongOldPassword() {
        User user = user("old-hash");
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("user@mail.ru", new ChangePasswordRequest("wrong", "new-password")))
                .isInstanceOf(PasswordIsIncorrect.class);
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(refreshSessionRepository);
    }

    @Test
    void rejectsNullGroup() {
        user("hash");

        assertThatThrownBy(() -> userService.changeGroup("user@mail.ru", new ChangeGroupRequest(null)))
                .isInstanceOf(GroupNotFoundException.class);
        verifyNoInteractions(scheduleService);
    }

    @Test
    void oauthUserCanSetFirstPasswordWithoutOldOne() {
        User user = user(null);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        userService.changePassword("user@mail.ru", new ChangePasswordRequest(null, "new-password"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
        assertThat(userService.getCurrentUser("user@mail.ru").hasPassword()).isTrue();
    }

    @Test
    void rejectsBlankNewPassword() {
        User user = user("old-hash");

        assertThatThrownBy(() -> userService.changePassword("user@mail.ru", new ChangePasswordRequest("old", "  ")))
                .isInstanceOf(PasswordIsIncorrect.class);
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void missingOldPasswordIsRejectedWhenPasswordExists() {
        User user = user("old-hash");

        assertThatThrownBy(() -> userService.changePassword("user@mail.ru", new ChangePasswordRequest(null, "new-password")))
                .isInstanceOf(PasswordIsIncorrect.class);
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void deleteAccountRemovesAllUserDataBeforeUser() {
        User user = user("hash");
        user.setId(7L);

        userService.deleteAccount("user@mail.ru");

        InOrder order = inOrder(absenceRepository, oauthAccountRepository, refreshSessionRepository,
                emailVerificationTokenRepository, passwordResetTokenRepository, userRepository);
        order.verify(absenceRepository).deleteAllByUser_Id(7L);
        order.verify(oauthAccountRepository).deleteAllByUser_Id(7L);
        order.verify(refreshSessionRepository).deleteAllByUser_Id(7L);
        order.verify(emailVerificationTokenRepository).deleteAllByUser_Id(7L);
        order.verify(passwordResetTokenRepository).deleteAllByUser_Id(7L);
        order.verify(userRepository).delete(user);
    }

    @Test
    void rejectsShortNewPassword() {
        User user = user("old-hash");

        assertThatThrownBy(() -> userService.changePassword("user@mail.ru", new ChangePasswordRequest("old", "short")))
                .isInstanceOf(PasswordIsIncorrect.class)
                .hasMessageContaining("8");
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }
}

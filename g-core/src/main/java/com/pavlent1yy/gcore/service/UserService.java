package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.GroupNotFoundException;
import com.pavlent1yy.gcore.customExceptions.PasswordIsIncorrect;
import com.pavlent1yy.gcore.dto.records.ChangeGroupRequest;
import com.pavlent1yy.gcore.dto.records.ChangePasswordRequest;
import com.pavlent1yy.gcore.dto.records.UserResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.AbsenceRepository;
import com.pavlent1yy.gcore.repository.EmailVerificationTokenRepository;
import com.pavlent1yy.gcore.repository.PasswordResetTokenRepository;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserOAuthAccountRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ScheduleService scheduleService;
    private final PasswordEncoder passwordEncoder;
    private final AbsenceRepository absenceRepository;
    private final UserOAuthAccountRepository oauthAccountRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private User getUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Пользователь не найден"
                ));
    }

    public UserResponse getCurrentUser(String email) {
        User user = getUserByEmail(email);

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getGroup(),
                user.getDepartment(),
                user.getRole(),
                user.getPasswordHash() != null
        );
    }

    @Transactional
    public User changePassword(String email, ChangePasswordRequest passwordRequest){
        User user = getUserByEmail(email);
        String newPassword = passwordRequest.newPassword();
        PasswordPolicy.validate(newPassword);
        if (user.getPasswordHash() == null) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            refreshSessionRepository.deleteAllByUser_Id(user.getId());
            return user;
        }
        if (passwordRequest.oldPassword() != null
                && passwordEncoder.matches(passwordRequest.oldPassword(), user.getPasswordHash())){
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            refreshSessionRepository.deleteAllByUser_Id(user.getId());
            return user;
        } else{
            throw new PasswordIsIncorrect("Неверный текущий пароль");
        }
    }

    public void changeGroup(String email, ChangeGroupRequest changeGroupRequest){
        User user = getUserByEmail(email);
        String newGroup = changeGroupRequest.newGroup();
        if (newGroup == null || !scheduleService.getAllGroups().contains(newGroup)) {
            throw new GroupNotFoundException("Группа не найдена: " + newGroup);
        }
        user.setGroup(newGroup);
        user.setDepartment(scheduleService.getDepartmentsByGroup(newGroup));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = getUserByEmail(email);
        Long userId = user.getId();

        absenceRepository.deleteAllByUser_Id(userId);
        oauthAccountRepository.deleteAllByUser_Id(userId);
        refreshSessionRepository.deleteAllByUser_Id(userId);
        emailVerificationTokenRepository.deleteAllByUser_Id(userId);
        passwordResetTokenRepository.deleteAllByUser_Id(userId);
        userRepository.delete(user);
    }
}

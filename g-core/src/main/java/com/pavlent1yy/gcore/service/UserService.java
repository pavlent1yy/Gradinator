package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.GroupNotFoundException;
import com.pavlent1yy.gcore.customExceptions.PasswordIsIncorrect;
import com.pavlent1yy.gcore.dto.records.ChangeGroupRequest;
import com.pavlent1yy.gcore.dto.records.ChangePasswordRequest;
import com.pavlent1yy.gcore.dto.records.UserResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.AbsenceRepository;
import com.pavlent1yy.gcore.repository.EmailVerificationTokenRepository;
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
                user.getRole()
        );
    }

    public void changePassword(String email, ChangePasswordRequest passwordRequest){
        User user = getUserByEmail(email);
        if (user.getPasswordHash() == null) {
            throw new PasswordIsIncorrect("У аккаунта нет пароля: вход выполнен через Google/GitHub");
        }
        if (passwordEncoder.matches(passwordRequest.oldPassword(), user.getPasswordHash())){
            user.setPasswordHash(passwordEncoder.encode(passwordRequest.newPassword()));
            userRepository.save(user);
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
        userRepository.delete(user);
    }
}

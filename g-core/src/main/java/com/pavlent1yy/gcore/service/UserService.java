package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.PasswordIsIncorrect;
import com.pavlent1yy.gcore.dto.records.ChangeGroupRequest;
import com.pavlent1yy.gcore.dto.records.ChangePasswordRequest;
import com.pavlent1yy.gcore.dto.records.UserResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                user.getRole()
        );
    }

    public void changePassword(String email, ChangePasswordRequest passwordRequest){
        User user = getUserByEmail(email);
        if (passwordEncoder.matches(passwordRequest.oldPassword(), user.getPasswordHash())){
            user.setPasswordHash(passwordEncoder.encode(passwordRequest.newPassword()));
            userRepository.save(user);
        } else{
            throw new PasswordIsIncorrect("Password is incorrect!");
        }
    }

    public void changeGroup(String email, ChangeGroupRequest changeGroupRequest){
        User user = getUserByEmail(email);
        user.setGroup(changeGroupRequest.newGroup());
        userRepository.save(user);
    }
}

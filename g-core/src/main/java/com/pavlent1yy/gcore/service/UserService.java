package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.dto.UserResponse;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Пользователь не найден"
                ));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getGroup(),
                user.getRole()
        );
    }
}

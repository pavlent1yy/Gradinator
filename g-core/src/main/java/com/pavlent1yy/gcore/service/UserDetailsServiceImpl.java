package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.client.UserDetailsImpl;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Attempt to load user by email: {}", email);

        return userRepository.findByEmail(email)
                .map(user -> {
                    log.debug("User found: email={}, id={}", user.getEmail(), user.getId());
                    return new UserDetailsImpl(user);
                })
                .orElseThrow(() -> {
                    log.warn("User not found during authentication: email={}", email);
                    return new UsernameNotFoundException("Invalid credentials");
                });
    }
}

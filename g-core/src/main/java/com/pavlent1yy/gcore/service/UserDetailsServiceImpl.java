package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.config.UserDetailsImpl;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.debug("Attempt to load user for authentication");

        return userRepository.findByEmail(EmailNormalizer.normalize(email))
                .map(user -> {
                    log.debug("User found for authentication");
                    return new UserDetailsImpl(user);
                })
                .orElseThrow(() -> {
                    log.warn("User not found during authentication");
                    return new UsernameNotFoundException("Invalid credentials");
                });
    }
}

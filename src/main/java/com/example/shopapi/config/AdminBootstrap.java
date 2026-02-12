package com.example.shopapi.config;

import com.example.shopapi.entity.Role;
import com.example.shopapi.entity.User;
import com.example.shopapi.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrap {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.email}")
    private String adminEmail;

    @Bean
    public CommandLineRunner initAdmin() {
        return args -> {
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = User.builder()
                        .username(adminUsername)
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .firstName("System")
                        .lastName("Administrator")
                        .role(Role.ROLE_ADMIN)
                        .enabled(true)
                        .emailVerified(true)
                        .accountNonLocked(true)
                        .build();

                userRepository.save(admin);
                log.info("Admin user created successfully: {}", adminUsername);
            } else {
                log.info("Admin user already exists: {}", adminUsername);
            }
        };
    }
}


package com.nodatanolife.bifusion_be.global.config;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;

    private static final String DELETED_USER_EMAIL = "deleted@bifusion.com";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByEmail(DELETED_USER_EMAIL).orElseGet(() -> {
            log.info("탈퇴 계정 생성");
            return userRepository.save(User.createDeletedUser());
        });
    }
}

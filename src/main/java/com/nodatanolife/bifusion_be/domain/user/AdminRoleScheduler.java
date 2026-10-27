package com.nodatanolife.bifusion_be.domain.user;

import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * app.admin.emails에 있는 계정을 관리자(role = ADMIN)로 올린다.
 * 기동 직후 한 번 돌고 이후 10분마다 돌아서, 배포 뒤에 가입한 계정도 다음 주기에 관리자가 된다.
 * 올리기만 하고 내리지는 않는다. 관리자 회수는 DB에서 직접 한다.
 */
@Slf4j
@Component
public class AdminRoleScheduler {

    private final UserRepository userRepository;
    private final List<String> adminEmails;

    public AdminRoleScheduler(UserRepository userRepository,
                              @Value("${app.admin.emails:}") List<String> adminEmails) {
        this.userRepository = userRepository;
        this.adminEmails = adminEmails.stream()
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .toList();
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "PT10M")
    @Transactional
    public void promoteAdmins() {
        if (adminEmails.isEmpty()) {
            return;
        }
        int count = userRepository.promoteToAdmin(adminEmails);
        if (count > 0) {
            log.info("[관리자 권한 스케줄러] ADMIN으로 변경한 계정 수: {}", count);
        }
    }
}

package com.nodatanolife.bifusion_be.domain.project;

import com.nodatanolife.bifusion_be.domain.project.repository.ProjectInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectInvitationScheduler {

    private static final int INVITATION_EXPIRE_DAYS = 7;

    private final ProjectInvitationRepository projectInvitationRepository;

    @Scheduled(cron = "0 0 * * * *") // 매시 정각
    @Transactional
    public void expireInvitations() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusDays(INVITATION_EXPIRE_DAYS);
        int count = projectInvitationRepository.bulkExpire(expiredBefore);
        log.info("[초대 만료 스케줄러] 만료 처리 건수: {}", count);
    }
}
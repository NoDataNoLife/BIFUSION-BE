package com.nodatanolife.bifusion_be.domain.recruitment.dto.response;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentResponse(
        Long recruitmentId,
        String jobTitle,
        String organization,
        LocalDate deadline,
        List<String> tags,
        LocalDateTime createdAt
) {
    public static RecruitmentResponse from(Recruit recruit, List<String> tags) {
        return new RecruitmentResponse(
                recruit.getRecruitId(),
                recruit.getJobTitle(),
                recruit.getOrganization(),
                recruit.getDeadline(),
                tags,
                recruit.getCreatedAt()
        );
    }
}
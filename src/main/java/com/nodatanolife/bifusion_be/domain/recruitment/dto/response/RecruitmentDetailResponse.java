package com.nodatanolife.bifusion_be.domain.recruitment.dto.response;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;

import java.time.LocalDate;
import java.util.List;

public record RecruitmentDetailResponse(
        Long recruitmentId,
        String jobTitle,
        String organization,
        List<String> tags,
        LocalDate deadline,
        String description,
        List<String> requirements,
        List<String> responsibilities,
        List<String> benefits
) {
    public static RecruitmentDetailResponse from(Recruit recruit, List<String> tags) {
        return new RecruitmentDetailResponse(
                recruit.getRecruitId(),
                recruit.getJobTitle(),
                recruit.getOrganization(),
                tags,
                recruit.getDeadline(),
                recruit.getDescription(),
                recruit.getRequirements(),
                recruit.getResponsibilities(),
                recruit.getBenefits()
        );
    }
}

package com.nodatanolife.bifusion_be.domain.recruitment.dto.response;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;

import java.time.LocalDate;
import java.util.List;

public record RecruitmentListResponse(
        Long recruitmentId,
        String jobTitle,
        String organization,
        List<String> tags,
        Integer currentMembers,
        Integer teamSize,
        LocalDate deadline
) {
    public static RecruitmentListResponse from(Recruit recruit, List<String> tags, Integer currentMembers) {
        return new RecruitmentListResponse(
                recruit.getRecruitId(),
                recruit.getJobTitle(),
                recruit.getOrganization(),
                tags,
                currentMembers,
                recruit.getTeamSize(),
                recruit.getDeadline()
        );
    }
}

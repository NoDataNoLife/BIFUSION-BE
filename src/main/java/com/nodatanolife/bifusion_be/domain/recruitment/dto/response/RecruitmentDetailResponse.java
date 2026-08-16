package com.nodatanolife.bifusion_be.domain.recruitment.dto.response;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;
import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplication;

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
        List<String> benefits,
        AuthorResponse author,
        List<ApplicationResponse> applications
) {
    public static RecruitmentDetailResponse from(Recruit recruit, List<String> tags, List<RecruitApplication> applications) {
        AuthorResponse authorResponse = AuthorResponse.from(recruit);

        List<ApplicationResponse> applicationResponses = applications.stream()
                .map(ApplicationResponse::from)
                .toList();

        return new RecruitmentDetailResponse(
                recruit.getRecruitId(),
                recruit.getJobTitle(),
                recruit.getOrganization(),
                tags,
                recruit.getDeadline(),
                recruit.getDescription(),
                recruit.getRequirements(),
                recruit.getResponsibilities(),
                recruit.getBenefits(),
                authorResponse,
                applicationResponses
        );
    }
    public record AuthorResponse(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {
        public static AuthorResponse from(Recruit recruit) {
            return new AuthorResponse(
                    recruit.getUser().getUserId(),
                    recruit.getUser().getNickname(),
                    recruit.getUser().getProfileImageUrl()
            );
        }
    }

    // 👈 [추가] 지원자 개별 정보를 담는 record
    public record ApplicationResponse(
            Long applicationId,
            Long userId,
            String nickname,
            String profileImageUrl,
            String status
    ) {
        public static ApplicationResponse from(RecruitApplication application) {
            return new ApplicationResponse(
                    application.getRecruitApplicationId(),
                    application.getUser().getUserId(),
                    application.getUser().getNickname(),
                    application.getUser().getProfileImageUrl(),
                    application.getStatus().name()
            );
        }
    }
}

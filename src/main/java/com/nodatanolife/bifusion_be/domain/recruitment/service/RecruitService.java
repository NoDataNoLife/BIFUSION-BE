package com.nodatanolife.bifusion_be.domain.recruitment.service;

import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitApplicationStatusRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitApplicationStatusResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitmentRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitmentResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitApplyRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitApplyResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface RecruitService {

    void anonymizeByUser(User user, User deletedUser);

    RecruitmentResponse createRecruitment(RecruitmentRequest request, User user);

    RecruitApplyResponse applyToRecruitment(Long recruitId, RecruitApplyRequest request, Long userId);

    RecruitApplicationStatusResponse updateApplicationStatus(
            Long recruitmentId,
            Long applicationId,
            RecruitApplicationStatusRequest request,
            Long userId
    );
}

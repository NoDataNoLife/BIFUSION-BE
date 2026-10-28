package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.AdminExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertPerformanceResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskGroupedResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ExpertService {

    void deleteApplicationByUser(User user);

    void anonymizeByUser(User user, User deletedUser);

    ExpertApplicationResponse apply(Long userId, MultipartFile certificationFile);

    ExpertApplicationStatus resolveStatus(User user);

    ExpertPerformanceResponse getMyPerformance(User user);

    ExpertTaskGroupedResponse getMyTasks(User user);

    Page<AdminExpertApplicationResponse> getApplications(ExpertApplicationStatus status, Pageable pageable);

    void approve(Long adminId, Long applicationId);

    void reject(Long adminId, Long applicationId, String reason);
}

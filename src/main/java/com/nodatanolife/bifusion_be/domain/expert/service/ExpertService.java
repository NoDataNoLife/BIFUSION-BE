package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertPerformanceResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface ExpertService {

    void deleteApplicationByUser(User user);

    void anonymizeByUser(User user, User deletedUser);

    ExpertApplicationResponse apply(Long userId, MultipartFile certificationFile);

    ExpertApplicationStatus resolveStatus(User user);

    ExpertPerformanceResponse getMyPerformance(User user);
}

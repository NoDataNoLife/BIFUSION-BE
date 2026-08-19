package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskStartResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ExpertTaskService {

    ExpertTaskStartResponse startTask(User expert, Long taskId);
}

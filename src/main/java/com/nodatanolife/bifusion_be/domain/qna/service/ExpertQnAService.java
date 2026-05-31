package com.nodatanolife.bifusion_be.domain.qna.service;

import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnaRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnaResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ExpertQnAService {

    void anonymizeByUser(User user, User deletedUser);

    ExpertQnaResponse createQna(Long userId, ExpertQnaRequest request);

}

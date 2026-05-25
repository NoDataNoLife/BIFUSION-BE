package com.nodatanolife.bifusion_be.domain.qna.service;

import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnAAnswerRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnAAnswerResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ExpertQnAService {

    void anonymizeByUser(User user, User deletedUser);

    ExpertQnAAnswerResponse createAnswer(Long userId, Long qnaId, ExpertQnAAnswerRequest request);


}

package com.nodatanolife.bifusion_be.domain.qna.service;

import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnADetailResponse;
import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnAAnswerRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnAAnswerResponse;
import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnaRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnaResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnAListResponse;
import org.springframework.data.domain.Page;

public interface ExpertQnAService {

    void anonymizeByUser(User user, User deletedUser);

    ExpertQnAAnswerResponse createAnswer(Long userId, Long qnaId, ExpertQnAAnswerRequest request);

    ExpertQnaResponse createQna(Long userId, ExpertQnaRequest request);

    Page<ExpertQnAListResponse> getQnaList(String sort, String keyword, int page, int size);

    ExpertQnADetailResponse getQnaDetail(Long qnaId);

    void deleteQna(Long userId, Long qnaId);
}

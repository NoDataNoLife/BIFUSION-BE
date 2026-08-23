package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface InspectionService {

    InspectionResponse createInspection(User user, InspectionCreateRequest request);

    InspectionDetailResponse getInspectionDetail(User expert, Long inspectionId);
}

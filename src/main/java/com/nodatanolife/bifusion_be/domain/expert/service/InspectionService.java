package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDecisionResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDraftResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionImageCommentResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface InspectionService {

    InspectionResponse createInspection(User user, InspectionCreateRequest request);

    /**
     * 신청자 취소. 전문가가 검수를 시작하기 전에만 가능하며 포인트를 환불한다.
     */
    InspectionResponse cancelInspection(User user, Long inspectionRequestId);

    /**
     * 검수 대상 삭제 전 정리. 시작 전 신청은 취소·환불하고, 전문가가 검수를 시작했으면 삭제를 막는다(409).
     */
    void cancelForTargetDeletion(Long userId, InspectionTargetType targetType, Long targetId);

    /**
     * 배정 없이 대기 중으로 남은 신청을 취소·환불한다 (일회성 정리용). 그새 상태가 바뀌었으면 건너뛴다.
     *
     * @return 취소했으면 true
     */
    boolean cancelUnassigned(Long inspectionRequestId);

    InspectionDetailResponse getInspectionDetail(User expert, Long inspectionId);

    InspectionDraftResponse saveDraftComment(User expert, Long inspectionId, String expertComment);

    InspectionImageCommentResponse saveImageComment(User expert, Long inspectionId, Long imageId, String comment);

    InspectionDecisionResponse approveInspection(User expert, Long inspectionId, String expertComment);

    InspectionDecisionResponse rejectInspection(User expert, Long inspectionId, String expertComment);
}

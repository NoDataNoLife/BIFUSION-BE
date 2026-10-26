package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDecisionResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDraftResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionImageCommentResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.MyInspectionRequestDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.MyInspectionRequestResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionProgressStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.Map;

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

    /**
     * 신청자의 "내가 요청한 검수" 목록. 취소된 신청은 제외한다.
     *
     * @param targetType null이면 전체
     * @param status     null이면 전체
     * @param keyword    대상 이름 검색어. null·공백이면 전체
     */
    Page<MyInspectionRequestResponse> getMyRequests(User user, InspectionTargetType targetType, InspectionProgressStatus status,
                                                    String keyword, int page, int size);

    /**
     * 신청자 목록 상단 요약용 상태별 건수. 검색·필터와 무관한 본인 전체 신청(취소 제외) 기준이며 네 상태를 항상 담는다.
     */
    Map<InspectionProgressStatus, Long> getMyRequestCounts(User user);

    MyInspectionRequestDetailResponse getMyRequestDetail(User user, Long inspectionRequestId);

    InspectionDetailResponse getInspectionDetail(User expert, Long inspectionId);

    InspectionDraftResponse saveDraftComment(User expert, Long inspectionId, String expertComment);

    InspectionImageCommentResponse saveImageComment(User expert, Long inspectionId, Long imageId, String comment);

    InspectionDecisionResponse approveInspection(User expert, Long inspectionId, String expertComment);

    InspectionDecisionResponse rejectInspection(User expert, Long inspectionId, String expertComment);
}

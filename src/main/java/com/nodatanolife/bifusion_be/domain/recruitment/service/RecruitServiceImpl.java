package com.nodatanolife.bifusion_be.domain.recruitment.service;

import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitmentDetailResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitApplicationStatusRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitApplicationStatusResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplicationStatus;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitmentRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitmentResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitmentListResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;
import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitApplicationRepository;
import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitRepository;
import com.nodatanolife.bifusion_be.domain.tag.entity.Tag;
import com.nodatanolife.bifusion_be.domain.tag.entity.TagMapping;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagMappingRepository;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitApplyRequest;   
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitApplyResponse; 
import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplication;         
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;               
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;                   
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;                          
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitServiceImpl implements RecruitService {

    private final RecruitRepository recruitRepository;
    private final RecruitApplicationRepository recruitApplicationRepository;
    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 회원 탈퇴 시 채용 공고 및 지원 내역 익명화
     * Recruit, RecruitApplication의 작성자/지원자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        recruitRepository.findAllByUser(user)
                .forEach(r -> r.anonymize(deletedUser));
        recruitApplicationRepository.findAllByUser(user)
                .forEach(r -> r.anonymize(deletedUser));
    }

    @Override
    public RecruitmentResponse createRecruitment(RecruitmentRequest request, User user) {
        Recruit recruit = Recruit.create(
                user,
                request.getJobTitle(),
                request.getOrganization(),
                request.getDescription(),
                request.getRequirements(),
                request.getResponsibilities(),
                request.getBenefits(),
                request.getTeamSize(),
                request.getDeadline()
        );
        recruitRepository.save(recruit);

        List<String> tagNames = new ArrayList<>();
        if (request.getTags() != null) {
            for (String tagName : request.getTags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.create(tagName)));
                tagMappingRepository.save(TagMapping.create(tag, recruit.getRecruitId(), "RECRUIT"));
                tagNames.add(tag.getName());
            }
        }

        return RecruitmentResponse.from(recruit, tagNames);
    }

    @Override
    public RecruitApplyResponse applyToRecruitment(Long recruitId, RecruitApplyRequest request, Long userId) {

        Recruit recruit = recruitRepository.findById(recruitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_NOT_FOUND));

        if (recruitApplicationRepository.existsByRecruit_RecruitIdAndUser_UserId(recruitId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        RecruitApplication application = RecruitApplication.create(
                recruit,
                user,
                request.getFullName(),
                request.getEmail(),
                request.getResumeUrl(),
                request.getMotivation()
        );
        recruitApplicationRepository.save(application);

        notificationService.create(
                recruit.getUser(),
                NotificationType.RECRUITMENT_APPLY,
                "새로운 지원서가 도착했습니다",
                user.getNickname() + "님이 \"" + recruit.getJobTitle() + "\" 공고에 지원했습니다.",
                TargetType.RECRUITMENT,
                recruit.getRecruitId()
        );

        return RecruitApplyResponse.from(application);
    }

    @Override
    public RecruitApplicationStatusResponse updateApplicationStatus(
            Long recruitmentId,
            Long applicationId,
            RecruitApplicationStatusRequest request,
            Long userId
    ) {
        // 모집글을 잠가 같은 모집글의 상태 변경을 한 번에 하나씩 처리한다.
        // 같은 지원서가 두 번 처리되거나, 동시에 수락해 정원을 넘기지 않게 한다.
        Recruit recruit = recruitRepository.findByIdForUpdate(recruitmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_NOT_FOUND));

        RecruitApplication application = recruitApplicationRepository.findById(applicationId)
                .filter(found -> found.getRecruit().getRecruitId().equals(recruit.getRecruitId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!recruit.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_ACCESS_DENIED);
        }

        if (request.getStatus() == RecruitApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS);
        }

        // 수락·거절은 대기 중인 지원서에 한 번만 한다 (결과 알림이 중복되거나 뒤집히지 않게)
        if (application.getStatus() != RecruitApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        // 정원 체크: ACCEPTED로 변경하는 경우에만
        if (request.getStatus() == RecruitApplicationStatus.ACCEPTED) {
            Integer teamSize = recruit.getTeamSize();

            if (teamSize != null) {
                long acceptedCount = recruitApplicationRepository
                        .countByRecruit_RecruitIdAndStatus(recruit.getRecruitId(), RecruitApplicationStatus.ACCEPTED);

                if (acceptedCount >= teamSize) {
                    throw new BusinessException(ErrorCode.RECRUITMENT_FULL);
                }
            }
        }

        application.updateStatus(request.getStatus());

        NotificationType notificationType = request.getStatus() == RecruitApplicationStatus.ACCEPTED
                ? NotificationType.RECRUITMENT_ACCEPTED
                : NotificationType.RECRUITMENT_REJECTED;
        String title = request.getStatus() == RecruitApplicationStatus.ACCEPTED
                ? "지원 합격 안내"
                : "지원 결과 안내";
        String content = "\"" + application.getRecruit().getJobTitle() + "\" 공고 지원 결과: "
                + (request.getStatus() == RecruitApplicationStatus.ACCEPTED ? "합격하셨습니다." : "아쉽게도 불합격하셨습니다.");

        notificationService.create(
                application.getUser(),
                notificationType,
                title,
                content,
                TargetType.RECRUITMENT,
                application.getRecruit().getRecruitId()
        );


        return RecruitApplicationStatusResponse.from(application);
    }

    @Override
    @Transactional(readOnly = true)
    public RecruitmentDetailResponse getRecruitmentDetail(Long recruitmentId) {

        Recruit recruit = recruitRepository.findById(recruitmentId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_NOT_FOUND));

        List<String> tags = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("RECRUIT", List.of(recruitmentId)).stream()
                .map(tm -> tm.getTag().getName())
                .toList();

        List<RecruitApplication> applications = recruitApplicationRepository
                .findAllByRecruit_RecruitId(recruitmentId);

        return RecruitmentDetailResponse.from(recruit, tags, applications);
    }
  
    @Override
    @Transactional(readOnly = true)
    public Page<RecruitmentListResponse> getRecruitmentList(String sort, int page, int size) {

        Sort sortOption = "DEADLINE_SOON".equalsIgnoreCase(sort)
                ? Sort.by("deadline").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sortOption);
        Page<Recruit> recruits = recruitRepository.findAllByIsPublicTrue(pageable);


        List<Long> recruitIds = recruits.getContent().stream()
                .map(Recruit::getRecruitId)
                .toList();

        Map<Long, List<String>> tagMap = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("RECRUIT", recruitIds).stream()
                .collect(Collectors.groupingBy(
                        TagMapping::getTargetId,
                        Collectors.mapping(tm -> tm.getTag().getName(), Collectors.toList())
                ));

        Map<Long, Integer> memberCountMap = recruitApplicationRepository
                .countByRecruitIdsAndStatus(recruitIds, RecruitApplicationStatus.ACCEPTED).stream()
                .collect(Collectors.toMap(
                        RecruitApplicationRepository.RecruitMemberCount::getRecruitId,
                        rc -> rc.getCount().intValue()
                ));

        return recruits.map(recruit -> RecruitmentListResponse.from(
                recruit,
                tagMap.getOrDefault(recruit.getRecruitId(), List.of()),
                memberCountMap.getOrDefault(recruit.getRecruitId(), 0)
        ));
    }

    @Transactional
    public void deleteRecruitment(Long recruitmentId, Long userId) {
        Recruit recruit = recruitRepository.findById(recruitmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECRUITMENT_NOT_FOUND)); // 또는 기존 컨벤션에 맞는 코드

        if (!recruit.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RECRUITMENT_ACCESS); // 없으면 신설
        }

        recruit.delete();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recruit> getPublicRecruitsByUser(Long userId) {
        return recruitRepository.findPublicByUserId(userId);
    }
}

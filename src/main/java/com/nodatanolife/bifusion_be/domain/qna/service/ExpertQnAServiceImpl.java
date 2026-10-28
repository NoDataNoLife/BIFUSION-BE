package com.nodatanolife.bifusion_be.domain.qna.service;

import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnADetailResponse;
import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnAAnswerRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnAAnswerResponse;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;
import com.nodatanolife.bifusion_be.domain.qna.repository.ExpertQnAAnswerRepository;
import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnaRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnaResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.expert.service.ExpertService;
import com.nodatanolife.bifusion_be.domain.tag.entity.Tag;
import com.nodatanolife.bifusion_be.domain.tag.entity.TagMapping;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagMappingRepository;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagRepository;
import com.nodatanolife.bifusion_be.domain.user.config.PointProperties;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.domain.user.service.PointService;
import com.nodatanolife.bifusion_be.domain.qna.repository.ExpertQnARepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnAListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertQnAServiceImpl implements ExpertQnAService {

    private final ExpertQnARepository expertQnARepository;
    private final ExpertQnAAnswerRepository expertQnAAnswerRepository;
    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;
    private final UserRepository userRepository;
    private final ExpertService expertService;
    private final NotificationService notificationService;
    private final PointService pointService;
    private final PointProperties pointProperties;

    /**
     * 회원 탈퇴 시 전문가 Q&A 익명화
     * ExpertQnA의 작성자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        expertQnARepository.findAllByUser(user)
                .forEach(q -> q.anonymize(deletedUser));
    }

    @Override
    public ExpertQnAAnswerResponse createAnswer(Long userId, Long qnaId, ExpertQnAAnswerRequest request) {
        User user = userRepository.getReferenceById(userId);

        boolean isExpert = expertService.resolveStatus(user) == ExpertApplicationStatus.APPROVED;
        if (!isExpert) {
            throw new BusinessException(ErrorCode.EXPERT_ONLY);
        }

        // 질문 행을 잠가 동시 답변의 answerCount 증가가 사라지지 않게 하고, 삭제된 질문에는 답변하지 못하게 한다.
        ExpertQnA qna = expertQnARepository.findActiveForUpdate(qnaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        boolean ownQuestion = qna.getUser().getUserId().equals(userId);
        // 같은 질문에 이미 답변한 전문가, 본인 질문에 답변한 경우는 보상하지 않는다 (짧은 답변 반복으로 포인트를 모으지 못하게)
        boolean rewardable = !ownQuestion
                && !expertQnAAnswerRepository.existsByExpertQnAAndUser(qna, user);

        ExpertQnAAnswer answer = ExpertQnAAnswer.create(qna, user, request.getContent());
        expertQnAAnswerRepository.save(answer);

        if (rewardable) {
            pointService.earn(userId, pointProperties.qnaAnswerReward(),
                    RewardHistory.TYPE_QNA_ANSWER_REWARD, RewardHistory.TARGET_QNA, qna.getExpertQnAId());
        }

        qna.addExpertReply();

        // 본인 질문에 단 답변은 알리지 않는다
        if (!ownQuestion) {
            notificationService.create(
                    qna.getUser(),
                    NotificationType.QNA_ANSWERED,
                    "질문에 답변이 등록되었습니다",
                    "\"" + qna.getTitle() + "\" 질문에 전문가 답변이 등록되었습니다.",
                    TargetType.QNA,
                    qna.getExpertQnAId()
            );
        }

        return ExpertQnAAnswerResponse.from(answer, isExpert);
    }

    @Override
    public ExpertQnaResponse createQna(Long userId, ExpertQnaRequest request) {
        User user = userRepository.getReferenceById(userId);

        ExpertQnA qna = ExpertQnA.create(user, request.getTitle(), request.getContent());
        expertQnARepository.save(qna);

        // 포인트가 부족하면 400으로 막히고 질문도 롤백된다
        pointService.spend(userId, pointProperties.qnaQuestionCost(),
                RewardHistory.TYPE_QNA_QUESTION, RewardHistory.TARGET_QNA, qna.getExpertQnAId());

        List<String> tags = request.getTags() != null ? request.getTags() : List.of();

        tags.forEach(tagName -> {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.create(tagName)));

            tagMappingRepository.save(
                    TagMapping.create(tag, qna.getExpertQnAId(), "EXPERT_QNA")
            );
        });

        return ExpertQnaResponse.from(qna, tags);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ExpertQnAListResponse> getQnaList(String sort, String keyword, int page, int size) {
        Sort sorting = switch (sort) {
            case "LATEST" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "POPULAR" -> Sort.by(Sort.Direction.DESC, "answerCount");
            case "ANSWERED_FIRST" -> Sort.by(
                    Sort.Order.desc("hasExpertReply"),
                    Sort.Order.desc("createdAt")
            );            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };

        Pageable pageable = PageRequest.of(page, size, sorting);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;

        Page<ExpertQnA> qnaPage = expertQnARepository.findAllByCondition(kw, pageable);

        List<Long> ids = qnaPage.getContent()
                .stream()
                .map(ExpertQnA::getExpertQnAId)
                .toList();

        Map<Long, List<String>> tagMap = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("EXPERT_QNA", ids)
                .stream()
                .collect(Collectors.groupingBy(
                        TagMapping::getTargetId,
                        Collectors.mapping(tm -> tm.getTag().getName(), Collectors.toList())
                ));

        return qnaPage.map(qna ->
                ExpertQnAListResponse.from(qna, tagMap.getOrDefault(qna.getExpertQnAId(), List.of()))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertQnADetailResponse getQnaDetail(Long qnaId) {
        ExpertQnA qna = expertQnARepository.findById(qnaId)
                .filter(q -> !q.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        List<String> tags = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("EXPERT_QNA", List.of(qnaId))
                .stream()
                .map(tm -> tm.getTag().getName())
                .toList();

        List<ExpertQnAAnswer> answers = expertQnAAnswerRepository
                .findAllByExpertQnAAndDeletedAtIsNullOrderByCreatedAtAsc(qna);

        return ExpertQnADetailResponse.from(qna, tags, answers);
    }

    @Override
    public void deleteQna(Long userId, Long qnaId) {
        ExpertQnA qna = expertQnARepository.findById(qnaId)
                .filter(q -> !q.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        if (!qna.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        qna.delete();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpertQnA> getPublicQnasByUser(Long userId) {
        return expertQnARepository.findPublicByUserId(userId);
    }

    @Override
    public ExpertQnAAnswerResponse updateAnswer(Long userId, Long qnaId, Long answerId, ExpertQnAAnswerRequest request) {
        ExpertQnAAnswer answer = getActiveAnswer(qnaId, answerId);
        validateAnswerOwner(answer, userId);

        answer.update(request.getContent());

        boolean isExpert = expertService.resolveStatus(answer.getUser()) == ExpertApplicationStatus.APPROVED;
        return ExpertQnAAnswerResponse.from(answer, isExpert);
    }

    @Override
    public void deleteAnswer(Long userId, Long qnaId, Long answerId) {
        // 등록과 같은 락을 써서 answerCount가 꼬이지 않게 함
        ExpertQnA qna = expertQnARepository.findActiveForUpdate(qnaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        ExpertQnAAnswer answer = getActiveAnswer(qnaId, answerId);
        validateAnswerOwner(answer, userId);

        answer.delete();
        qna.removeExpertReply();
    }

    private ExpertQnAAnswer getActiveAnswer(Long qnaId, Long answerId) {
        return expertQnAAnswerRepository.findById(answerId)
                .filter(a -> !a.isDeleted())
                .filter(a -> a.getExpertQnA().getExpertQnAId().equals(qnaId))
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateAnswerOwner(ExpertQnAAnswer answer, Long userId) {
        if (!answer.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
    }

}

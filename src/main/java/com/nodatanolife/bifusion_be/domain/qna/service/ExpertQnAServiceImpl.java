package com.nodatanolife.bifusion_be.domain.qna.service;

import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnADetailResponse;
import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnAAnswerRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnAAnswerResponse;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;
import com.nodatanolife.bifusion_be.domain.qna.repository.ExpertQnAAnswerRepository;
import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnaRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnaResponse;
import com.nodatanolife.bifusion_be.domain.tag.entity.Tag;
import com.nodatanolife.bifusion_be.domain.tag.entity.TagMapping;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagMappingRepository;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagRepository;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
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

        if (!user.getIsExpert()) {
            throw new BusinessException(ErrorCode.EXPERT_ONLY);
        }

        ExpertQnA qna = expertQnARepository.findById(qnaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        ExpertQnAAnswer answer = ExpertQnAAnswer.create(qna, user, request.getContent());
        expertQnAAnswerRepository.save(answer);

        qna.addExpertReply();

        return ExpertQnAAnswerResponse.from(answer);
    }

    @Override
    public ExpertQnaResponse createQna(Long userId, ExpertQnaRequest request) {
        User user = userRepository.getReferenceById(userId);

        ExpertQnA qna = ExpertQnA.create(user, request.getTitle(), request.getContent());
        expertQnARepository.save(qna);

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
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        List<String> tags = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("EXPERT_QNA", List.of(qnaId))
                .stream()
                .map(tm -> tm.getTag().getName())
                .toList();

        List<ExpertQnAAnswer> answers = expertQnAAnswerRepository
                .findAllByExpertQnAOrderByCreatedAtAsc(qna);

        return ExpertQnADetailResponse.from(qna, tags, answers);
    }
}

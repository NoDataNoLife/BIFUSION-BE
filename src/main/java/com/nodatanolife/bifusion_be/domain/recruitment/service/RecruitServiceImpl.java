package com.nodatanolife.bifusion_be.domain.recruitment.service;

import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitmentRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitmentResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;
import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitApplicationRepository;
import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitRepository;
import com.nodatanolife.bifusion_be.domain.tag.entity.Tag;
import com.nodatanolife.bifusion_be.domain.tag.entity.TagMapping;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagMappingRepository;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitServiceImpl implements RecruitService {

    private final RecruitRepository recruitRepository;
    private final RecruitApplicationRepository recruitApplicationRepository;
    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;

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
}

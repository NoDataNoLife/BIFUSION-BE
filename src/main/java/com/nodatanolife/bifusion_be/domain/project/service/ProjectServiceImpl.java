package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.repository.ProjectFavoriteRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectInvitationRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectFavoriteRepository projectFavoriteRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;

    /**
     * 회원 탈퇴 시 프로젝트 관련 데이터 삭제
     * 즐겨찾기, 멤버, 초대 내역 삭제
     */
    @Override
    public void deleteByUser(User user) {
        projectFavoriteRepository.deleteByUser(user);
        projectMemberRepository.deleteByUser(user);
        projectInvitationRepository.deleteByInviterOrInvitee(user, user);
    }
}

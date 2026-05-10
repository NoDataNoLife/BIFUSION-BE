package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectRepository;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectUpdateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInfoResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectFavoriteRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectInvitationRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectFavoriteRepository projectFavoriteRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;


    /**
     * 프로젝트 생성
     */
    @Override
    public ProjectCreateResponse createProject(ProjectCreateRequest request, String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Project project = Project.of(
                request.title(),
                request.description(),
                request.isPublic()
        );
        Project savedProject = projectRepository.save(project);

        ProjectMember manager = ProjectMember.of(savedProject, owner, ProjectRole.MANAGER);
        projectMemberRepository.save(manager);

        return ProjectCreateResponse.of(savedProject, List.of(ProjectCreateResponse.MemberResponse.from(manager)));
    }

    /**
     * 프로젝트 기본 정보 수정
     */
    @Override
    @Transactional
    public ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, String email) {
    
        ProjectMember member = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

        if (member.getRole() != ProjectRole.MANAGER) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        project.update(request.title(), request.description(), request.bannerImageUrl());

        return ProjectUpdateResponse.of(project);
    }


    /**
     * 프로젝트 기본 정보 조회 
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectInfoResponse getProjectInfo(Long projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
      
        boolean isMember = projectMemberRepository
                .existsByProjectProjectIdAndUserUserId(projectId, user.getUserId());
        if (!isMember) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        List<ProjectMember> members = projectMemberRepository
                .findByProjectProjectId(projectId);

        return ProjectInfoResponse.of(project, members);
    }

   
    /**
     * 프로젝트 멤버 삭제 
     * 관리자가 본인을 삭제하는 경우 다른 관리자가 1명 이상 있어야 한다.
     */
    @Override
    @Transactional
    public void deleteMember(Long projectId, Long targetUserId, String email) {
        // 요청자 확인
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 요청자가 MANAGER인지 확인
        ProjectMember requesterMember = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, requester.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

        if (!"MANAGER".equals(requesterMember.getRole())) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 삭제 대상 멤버 확인
        ProjectMember targetMember = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        // 관리자가 본인을 삭제하는 경우 다른 관리자가 1명 이상 있어야 함
        if ("MANAGER".equals(targetMember.getRole())) {
            long managerCount = projectMemberRepository
                    .countByProjectProjectIdAndRole(projectId, "MANAGER");
            if (managerCount <= 1) {
                throw new BusinessException(ErrorCode.PROJECT_LAST_MANAGER);
            }
        }

        projectMemberRepository.delete(targetMember);
    }

  
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

    /**
     * 마이페이지에서 자신이 참여한 여러 프로젝트의 공개 여부를 일괄 변경
     */
    @Override
    public ProjectVisibilityUpdateResponse updateMyProjectVisibilities(
            Long userId,
            ProjectVisibilityUpdateRequest request
    ) {
        List<ProjectMember> updated = new ArrayList<>();

        for (ProjectVisibilityUpdateRequest.Item item : request.items()) {
            ProjectMember member = projectMemberRepository
                    .findByProjectProjectIdAndUserUserId(item.projectId(), userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

            // 비공개 프로젝트를 공개로 변경 시도 차단
            boolean projectIsPublic = Boolean.TRUE.equals(member.getProject().getIsPublic());
            if (item.isPublic() && !projectIsPublic) {
                throw new BusinessException(ErrorCode.PROJECT_VISIBILITY_PRIVATE_FORBIDDEN);
            }

            member.updateVisibility(item.isPublic());
            updated.add(member);
        }

        return ProjectVisibilityUpdateResponse.from(updated);
    }
}

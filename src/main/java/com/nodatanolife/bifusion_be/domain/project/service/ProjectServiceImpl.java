package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInfoResponse;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectRepository;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
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
    private final ProjectFavoriteRepository projectFavoriteRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectInvitationRepository projectInvitationRepository;


    /**
     * 프로젝트 생성
     */
    @Override
    @Transactional
    public ProjectCreateResponse createProject(ProjectCreateRequest request, String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Project project = Project.of(
                request.title(),
                request.description(),
                request.isPublic()
        );
        Project savedProject = projectRepository.save(project);

        List<ProjectCreateResponse.MemberResponse> memberResponses = new ArrayList<>();

        ProjectMember manager = ProjectMember.of(savedProject, owner, ProjectRole.MANAGER);
        projectMemberRepository.save(manager);
        memberResponses.add(new ProjectCreateResponse.MemberResponse(
                owner.getUserId(),
                owner.getNickname(),
                owner.getProfileImageUrl(),
                ProjectRole.MANAGER
        ));

        if (request.members() != null) {
            for (ProjectCreateRequest.MemberRequest memberReq : request.members()) {

                if (memberReq.userId().equals(owner.getUserId())) continue;

                User memberUser = userRepository.findById(memberReq.userId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

                ProjectMember projectMember = ProjectMember.of(
                        savedProject, memberUser, ProjectRole.MEMBER
                );
                projectMemberRepository.save(projectMember);

                memberResponses.add(new ProjectCreateResponse.MemberResponse(
                        memberUser.getUserId(),
                        memberUser.getNickname(),
                        memberUser.getProfileImageUrl(),
                        ProjectRole.MEMBER
                ));
            }
        }

        return ProjectCreateResponse.of(savedProject, memberResponses);
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
}

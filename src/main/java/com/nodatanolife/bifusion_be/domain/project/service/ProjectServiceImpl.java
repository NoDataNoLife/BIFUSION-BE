package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.dto.request.*;
import com.nodatanolife.bifusion_be.domain.project.dto.response.*;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.project.repository.*;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

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
    public ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, String email) {
      
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    
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
     * 내가 참여한 프로젝트 목록 조회 (커서 기반, 기본 4개)
     * MANAGER / MEMBER 역할 모두 포함
     * 정렬: joinedAt DESC, projectMemberId DESC (tiebreaker)
     */
    @Override
    @Transactional(readOnly = true)
    public MyProjectsResponse getMyProjects(String email, String cursor, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int pageSize = Math.max(1, Math.min(size, 50));

        LocalDateTime cursorJoinedAt = null;
        Long cursorMemberId = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(
                        Base64.getUrlDecoder().decode(cursor),
                        StandardCharsets.UTF_8
                );
                String[] parts = decoded.split(":");
                if (parts.length != 2) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }
                cursorJoinedAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(Long.parseLong(parts[0])),
                        ZoneOffset.UTC
                );
                cursorMemberId = Long.parseLong(parts[1]);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        List<ProjectMember> rows;
        if (cursorJoinedAt == null) {
            rows = projectMemberRepository.findMyProjectsFirstPage(
                    user.getUserId(),
                    PageRequest.of(0, pageSize + 1)
            );
        } else {
            rows = projectMemberRepository.findMyProjectsByCursor(
                    user.getUserId(),
                    cursorJoinedAt,
                    cursorMemberId,
                    PageRequest.of(0, pageSize + 1)
            );
        }

        boolean hasNext = rows.size() > pageSize;
        List<ProjectMember> page = hasNext ? rows.subList(0, pageSize) : rows;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            ProjectMember last = page.getLast();
            long epochMilli = last.getJoinedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
            String raw = epochMilli + ":" + last.getProjectMemberId();
            nextCursor = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        long totalCount = projectMemberRepository.countByUserUserId(user.getUserId());

        return MyProjectsResponse.of(page, nextCursor, hasNext, totalCount);
    }


    /**
     * 프로젝트 멤버 삭제
     * 관리자가 본인을 삭제하는 경우 다른 관리자가 1명 이상 있어야 한다.
     */
    @Override
    public void deleteMember(Long projectId, Long targetUserId, String email) {
        // 요청자 확인
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 요청자가 MANAGER인지 확인
        ProjectMember requesterMember = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, requester.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

        if (requesterMember.getRole() != ProjectRole.MANAGER) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 삭제 대상 멤버 확인
        ProjectMember targetMember = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        // 관리자가 본인을 삭제하는 경우 다른 관리자가 1명 이상 있어야 함
        if (targetMember.getRole() == ProjectRole.MANAGER) {
            long managerCount = projectMemberRepository
                    .countByProjectProjectIdAndRole(projectId, ProjectRole.MANAGER);
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
     * 마이페이지에서 내가 참여한 프로젝트들의 공개 여부 일괄 변경
     * 비공개 프로젝트는 공개로 변경 불가
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


    /**
     * 프로젝트 역할 변경
     */
    @Override
    public MemberRoleUpdateResponse updateMemberRole(Long projectId, Long targetUserId, MemberRoleUpdateRequest request, String email) {
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 요청자가 MANAGER인지 확인
        ProjectMember requesterMember = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, requester.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

        if (requesterMember.getRole() != ProjectRole.MANAGER) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 대상 멤버 확인
        ProjectMember targetMember = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        // 이미 동일한 역할인 경우
        if (targetMember.getRole().equals(request.role())) {
            throw new BusinessException(ErrorCode.PROJECT_SAME_ROLE);
        }

        targetMember.updateRole(request.role());

        return MemberRoleUpdateResponse.of(targetMember);
    }
    /**
     * 내 프로젝트 대시보드 조회
     */
    @Override
    @Transactional(readOnly = true)
    public MyProjectDashboardResponse getMyProjectDashboard(Long userId) {
        List<ProjectMember> memberships = projectMemberRepository.findAllByUserIdWithProjectAndMembers(userId);
        List<ProjectDetailResponse> managingProjects = memberships.stream()
                .filter(m -> m.getRole() == ProjectRole.MANAGER)                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());

        List<ProjectDetailResponse> participatingProjects = memberships.stream()
                .filter(m -> m.getRole() == ProjectRole.MEMBER)                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());

        return MyProjectDashboardResponse.of(managingProjects, participatingProjects);
    }

    /**
     * ProjectMember 엔티티를 ProjectDetailResponse DTO로 변환
     */
    private ProjectDetailResponse convertToDetailResponse(ProjectMember membership) {
        Project project = membership.getProject();

        List<ProjectMemberResponse> memberResponses = project.getProjectMembers().stream()                .map(pm -> new ProjectMemberResponse(
                        pm.getUser().getUserId(),
                        pm.getUser().getNickname(),
                        pm.getUser().getProfileImageUrl()
                ))
                .toList();

        return new ProjectDetailResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                membership.getRole(),
                project.getBannerImageUrl(),
                false,
                memberResponses.size(),
                memberResponses,
                project.getUpdatedAt()
        );
    }
}

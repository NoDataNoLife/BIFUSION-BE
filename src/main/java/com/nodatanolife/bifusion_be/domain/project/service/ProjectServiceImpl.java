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
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
import com.nodatanolife.bifusion_be.domain.file.service.S3ObjectCleaner;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.InferenceResultRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainedModelRepository;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class  ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectFavoriteRepository projectFavoriteRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final S3Service s3Service;
    private final S3ObjectCleaner s3ObjectCleaner;
    private final JobRepository jobRepository;
    private final FileRepository fileRepository;
    private final InferenceResultRepository inferenceResultRepository;
    private final AugmentationConfigRepository augmentationConfigRepository;
    private final TrainedModelRepository trainedModelRepository;
    private final InspectionService inspectionService;

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
      
        Project project = projectRepository.findByProjectIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    
        ProjectMember member = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

        if (member.getRole() != ProjectRole.MANAGER) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        project.update(request.title(), request.description());

        return ProjectUpdateResponse.of(project);
    }


    /**
     * 프로젝트 기본 정보 조회 
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectInfoResponse getProjectInfo(Long projectId, String email) {
        Project project = projectRepository.findByProjectIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
      
        boolean isMember = projectMemberRepository
                .existsByProjectProjectIdAndUserUserId(projectId, user.getUserId());
        if (!isMember) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        return ProjectInfoResponse.of(project, project.getProjectMembers());
    }


    /**
     * 내가 참여한 프로젝트 목록 조회 (커서 기반, 기본 4개)
     * MANAGER / MEMBER 역할 모두 포함
     * 정렬: joinedAt DESC, projectMemberId DESC (tiebreaker)
     */
    @Override
    @Transactional(readOnly = true)
    public MyProjectsResponse getMyProjects(String email, String cursor, int size, Boolean isPublic) {
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
                    isPublic,
                    PageRequest.of(0, pageSize + 1)
            );
        } else {
            rows = projectMemberRepository.findMyProjectsByCursor(
                    user.getUserId(),
                    isPublic,
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

        long totalCount = projectMemberRepository.countMyProjects(user.getUserId(), isPublic);

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

        // 마지막 관리자를 MEMBER로 내리면 관리자 없는 프로젝트가 되므로 차단
        if (targetMember.getRole() == ProjectRole.MANAGER) {
            long managerCount = projectMemberRepository
                    .countByProjectProjectIdAndRole(projectId, ProjectRole.MANAGER);
            if (managerCount <= 1) {
                throw new BusinessException(ErrorCode.PROJECT_LAST_MANAGER_DEMOTE);
            }
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
                .filter(m -> m.getRole() == ProjectRole.MANAGER)                .map(ProjectDetailResponse::from)
                .collect(Collectors.toList());

        List<ProjectDetailResponse> participatingProjects = memberships.stream()
                .filter(m -> m.getRole() == ProjectRole.MEMBER)                .map(ProjectDetailResponse::from)
                .collect(Collectors.toList());

        return MyProjectDashboardResponse.of(managingProjects, participatingProjects);
    }


    /**
     * 프로젝트 배너 등록/변경/삭제 (file이 없으면 삭제)
     * 새 이미지 업로드 → DB 반영 → 기존 파일 삭제 순서로 처리해, 중간에 실패해도 기존 배너가 남는다.
     * - 기존 파일은 커밋 뒤에 지운다. 삭제 실패는 로그만 남긴다 (새 배너는 이미 저장됨)
     * - 업로드 뒤 롤백되면 방금 올린 파일을 지운다
     */
    @Override
    public ProjectBannerUpdateResponse updateBannerImage(Long projectId, MultipartFile file, String email) {
        Project project = projectRepository.findByProjectIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ProjectMember member = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

        if (member.getRole() != ProjectRole.MANAGER) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        String oldUrl = project.getBannerImageUrl();

        // 새 이미지 업로드 or null 처리
        String newUrl = null;
        if (file != null && !file.isEmpty()) {
            newUrl = s3Service.uploadFile(file, "projects/banners");
            s3ObjectCleaner.deleteAfterRollback(Collections.singletonList(S3Service.extractKeyFromUrl(newUrl)));
        }

        project.updateBannerImage(newUrl);
        projectRepository.saveAndFlush(project);

        if (oldUrl != null) {
            s3ObjectCleaner.deleteAfterCommit(Collections.singletonList(S3Service.extractKeyFromUrl(oldUrl)));
        }
        return ProjectBannerUpdateResponse.of(project);
    }


    /**
     * 프로젝트 삭제 (MANAGER만 가능, soft delete)
     * - 진행 중(PENDING/RUNNING)인 작업이 있으면 409로 막는다
     * - 증강 데이터 검수 신청은 시작 전이면 취소·환불하고, 전문가가 시작했으면 409로 막는다
     * - 멤버·즐겨찾기·초대는 지운다. 목록·권한 확인이 멤버십 기준이라 삭제된 프로젝트는 더 이상 보이지 않는다
     * - 배너·Job 입력/결과 파일·증강 결과·학습 모델의 S3 객체는 커밋 뒤에 지운다 (releaseProjectFiles)
     * - Job·학습 모델·활동 로그 row는 그대로 둔다 (활동 로그 등이 프로젝트를 참조)
     */
    @Override
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectRepository.findByProjectIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        ProjectMember member = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));

        if (member.getRole() != ProjectRole.MANAGER) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        if (jobRepository.existsByProjectProjectIdAndStatusIn(projectId, List.of(JobStatus.PENDING, JobStatus.RUNNING))) {
            throw new BusinessException(ErrorCode.PROJECT_HAS_ACTIVE_JOB);
        }

        inspectionService.cancelForProjectDeletion(projectId);

        List<String> s3Keys = releaseProjectFiles(project);

        projectFavoriteRepository.deleteByProject(project);
        projectInvitationRepository.deleteByProject(project);
        project.getProjectMembers().clear();
        project.markDeleted();

        s3ObjectCleaner.deleteAfterCommit(s3Keys);
    }

    /**
     * 삭제할 프로젝트의 파일을 DB에서 정리하고, 커밋 뒤 지울 S3 key를 돌려준다.
     * - Job 입력·결과 파일 row를 지운다 (저장 용량 집계에서 빠진다). 이를 참조하는 추론 결과를 먼저 지운다.
     *   데이터셋에 첨부된 파일은 데이터셋이 계속 쓰므로 남긴다.
     * - 증강 결과는 삭제 처리한다 (검수 이력에는 삭제된 대상으로 보인다)
     * - 학습 모델 row는 남기고 모델 파일만 지운다
     * - 데이터셋에 첨부된 파일의 객체는 지우지 않는다
     */
    private List<String> releaseProjectFiles(Project project) {
        Long projectId = project.getProjectId();
        List<String> keys = new ArrayList<>();
        keys.add(S3Service.extractKeyFromUrl(project.getBannerImageUrl()));
        project.updateBannerImage(null);

        keys.addAll(fileRepository.findJobFileKeysByProjectId(projectId));
        inferenceResultRepository.deleteTop3SupportsByProjectId(projectId);
        inferenceResultRepository.deleteAllByProjectId(projectId);
        fileRepository.deleteJobFilesByProjectId(projectId);

        for (AugmentationConfig config : augmentationConfigRepository.findAllByJobProjectProjectId(projectId)) {
            if (config.isDeleted()) {
                continue;
            }
            keys.add(config.getDatasetKey());
            keys.add(config.getPreviewKey());
            config.deleteResult();
        }

        trainedModelRepository.findAllWithTrainingJobByProjectId(projectId)
                .forEach(model -> keys.add(model.getModelKey()));

        // 증강 결과 key가 데이터셋에 첨부된 파일과 같을 수 있다 (파일 row는 위 bulk delete에서 이미 제외)
        keys.removeIf(Objects::isNull);
        if (!keys.isEmpty()) {
            keys.removeAll(fileRepository.findDatasetAttachedKeys(keys));
        }
        return keys;
    }

}

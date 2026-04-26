package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.dto.response.InvitationRespondResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInvitationResponse;
import com.nodatanolife.bifusion_be.domain.project.entity.InvitationAction;
import com.nodatanolife.bifusion_be.domain.project.entity.InvitationStatus;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectInvitationRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectInvitationServiceImpl implements ProjectInvitationService {

    // 동일 PENDING 초대에 대한 재발송 쿨다운 (시간 단위).
    private static final int INVITATION_COOLDOWN_HOURS = 1;

    private static final String DEFAULT_ROLE = "MEMBER";

    private final ProjectInvitationRepository projectInvitationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MailService mailService;

    /**
     * 프로젝트 초대 생성
     * 1. ACCEPTED — 이미 멤버 INVITATION_ALREADY_MEMBER 에러처리
     * 2. PENDING  — 1시간 쿨다운, 통과 시 기존 row의 lastSentAt만 갱신 (재발송)
     * 3. EXPIRED / REJECTED — 새 초대 row 생성
     */
    @Override
    public ProjectInvitationResponse invite(Long projectId, Long inviterUserId, String rawEmail) {

        // 이메일은 소문자로 정규화
        String email = rawEmail.toLowerCase();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        User inviter = userRepository.findById(inviterUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 초대자는 해당 프로젝트의 멤버여야 한다
        if (!projectMemberRepository.existsByProjectProjectIdAndUserUserId(projectId, inviterUserId)) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 가입자/미가입자 분기에 사용 — 가입자라면 invitee_id를 함께 저장한다
        Optional<User> inviteeOpt = userRepository.findByEmail(email);

        // 이미 프로젝트 멤버라면 초대 자체가 의미 없으므로 차단
        inviteeOpt.ifPresent(invitee -> {
            if (projectMemberRepository.existsByProjectProjectIdAndUserUserId(projectId, invitee.getUserId())) {
                throw new BusinessException(ErrorCode.INVITATION_ALREADY_MEMBER);
            }
        });

        // 동일 (project, email)의 가장 최근 초대 1건 — 상태에 따라 재발송 or 새 row 결정
        Optional<ProjectInvitation> latestOpt = projectInvitationRepository
                .findFirstByProjectProjectIdAndInviteeEmailOrderByLastSentAtDesc(projectId, email);

        if (latestOpt.isPresent()) {
            ProjectInvitation latest = latestOpt.get();

            switch (latest.getStatus()) {
                case ACCEPTED ->
                    throw new BusinessException(ErrorCode.INVITATION_ALREADY_MEMBER);

                case PENDING -> {
                    // lastSentAt만 갱신해서 재발송으로 처리
                    LocalDateTime cooldownUntil = latest.getLastSentAt()
                            .plusHours(INVITATION_COOLDOWN_HOURS);
                    if (LocalDateTime.now().isBefore(cooldownUntil)) {
                        throw new BusinessException(ErrorCode.INVITATION_COOLDOWN);
                    }
                    latest.resend();
                    mailService.sendInvitationMail(email, project.getTitle(), inviter.getName());
                    return ProjectInvitationResponse.from(latest);
                }

                case EXPIRED, REJECTED -> {
                }
            }
        }

        // 가입자와 미가입자 분기하여 초대 row 생성
        ProjectInvitation invitation = inviteeOpt
                .map(invitee -> ProjectInvitation.ofRegistered(project, inviter, invitee, DEFAULT_ROLE))
                .orElseGet(() -> ProjectInvitation.ofUnregistered(project, inviter, email, DEFAULT_ROLE));

        ProjectInvitation saved = projectInvitationRepository.save(invitation);

        if (inviteeOpt.isPresent()) {
            mailService.sendInvitationMail(email, project.getTitle(), inviter.getName());
        } else {
            mailService.sendSignUpInvitationMail(email, project.getTitle(), inviter.getName());
        }
        return ProjectInvitationResponse.from(saved);
    }

    /**
     * 프로젝트 초대 수락/거절 처리
     */
    @Override
    public InvitationRespondResponse respond(Long projectId, Long invitationId, Long userId, InvitationAction action) {

        ProjectInvitation invitation = projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));

        // 1. 초대 존재 및 해당 프로젝트 소속 검증 — INVITATION_NOT_FOUND
        if (!invitation.getProject().getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_FOUND);
        }

        // 2. 본인(invitee) 초대 검증 — PROJECT_ACCESS_DENIED
        if (invitation.getInvitee() == null || !invitation.getInvitee().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        // 3. 만료 검증 — INVITATION_EXPIRED (스케줄러가 전환한 EXPIRED 상태)
        if (invitation.getStatus() == InvitationStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.INVITATION_EXPIRED);
        }

        // 4. PENDING 상태 검증 — INVITATION_ALREADY_RESPONDED (이미 수락/거절된 초대)
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVITATION_ALREADY_RESPONDED);
        }

        switch (action) {
            // 5. ACCEPT — 초대 status를 ACCEPTED로 갱신하고 ProjectMember row 생성
            case ACCEPT -> {
                invitation.accept();
                ProjectMember member = ProjectMember.of(
                        invitation.getProject(),
                        invitation.getInvitee(),
                        invitation.getRole()
                );
                projectMemberRepository.save(member);
            }
            // 6. REJECT — 초대 status를 REJECTED로 갱신
            case REJECT -> invitation.reject();
        }

        return InvitationRespondResponse.from(invitation);
    }
}

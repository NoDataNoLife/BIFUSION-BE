package com.nodatanolife.bifusion_be.project;

import com.nodatanolife.bifusion_be.domain.notification.dto.response.NotificationResponse;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.domain.project.dto.request.MemberRoleUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInvitationResponse;
import com.nodatanolife.bifusion_be.domain.project.entity.InvitationAction;
import com.nodatanolife.bifusion_be.domain.project.entity.InvitationStatus;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectInvitationService;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2UserService;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 초대 시 권한 지정, 마지막 관리자 권한 해제 방지, 알림창 초대 응답용 초대 정보.
 */
class ProjectInvitationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ProjectInvitationService projectInvitationService;
    @Autowired
    NotificationService notificationService;
    @Autowired
    CustomOAuth2UserService customOAuth2UserService;

    @Test
    void 역할을_지정하지_않으면_MEMBER로_초대된다() {
        User manager = user("manager@test.com", 0);
        user("invitee@test.com", 0);
        Long projectId = project(manager);

        ProjectInvitationResponse response = projectInvitationService.invite(projectId, manager.getUserId(), "invitee@test.com", null);

        assertThat(response.role()).isEqualTo(ProjectRole.MEMBER);
    }

    @Test
    void MANAGER는_MANAGER로_초대할_수_있고_수락하면_그_역할로_합류한다() {
        User manager = user("manager@test.com", 0);
        User invitee = user("invitee@test.com", 0);
        Long projectId = project(manager);

        ProjectInvitationResponse response = projectInvitationService.invite(projectId, manager.getUserId(), "invitee@test.com", ProjectRole.MANAGER);
        projectInvitationService.respond(projectId, response.projectInvitationId(), invitee.getUserId(), InvitationAction.ACCEPT);

        assertThat(response.role()).isEqualTo(ProjectRole.MANAGER);
        assertThat(projectMemberRepository.findByProjectProjectIdAndUserUserId(projectId, invitee.getUserId()).orElseThrow().getRole())
                .isEqualTo(ProjectRole.MANAGER);
    }

    @Test
    void MEMBER는_MANAGER로_초대할_수_없다() {
        User manager = user("manager@test.com", 0);
        User member = user("member@test.com", 0);
        user("invitee@test.com", 0);
        Long projectId = project(manager);
        addMember(projectId, member, ProjectRole.MEMBER);

        assertThatThrownBy(() -> projectInvitationService.invite(projectId, member.getUserId(), "invitee@test.com", ProjectRole.MANAGER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
        // MEMBER 초대는 그대로 가능
        assertThat(projectInvitationService.invite(projectId, member.getUserId(), "invitee@test.com", ProjectRole.MEMBER).role())
                .isEqualTo(ProjectRole.MEMBER);
    }

    @Test
    void 쿨다운이_지난_재발송은_요청한_역할로_갱신된다() {
        User manager = user("manager@test.com", 0);
        user("invitee@test.com", 0);
        Long projectId = project(manager);
        ProjectInvitationResponse first = projectInvitationService.invite(projectId, manager.getUserId(), "invitee@test.com", ProjectRole.MEMBER);
        jdbc.update("UPDATE project_invitation SET last_sent_at = now() - interval '2 hours' WHERE project_invitation_id = ?",
                first.projectInvitationId());

        ProjectInvitationResponse resent = projectInvitationService.invite(projectId, manager.getUserId(), "invitee@test.com", ProjectRole.MANAGER);

        assertThat(resent.projectInvitationId()).isEqualTo(first.projectInvitationId());
        assertThat(resent.role()).isEqualTo(ProjectRole.MANAGER);
    }

    @Test
    void 관리자가_한_명뿐이면_MEMBER로_내릴_수_없고_두_명이면_가능하다() {
        User manager = user("manager@test.com", 0);
        User other = user("other@test.com", 0);
        Long projectId = project(manager);

        assertThatThrownBy(() -> projectService.updateMemberRole(projectId, manager.getUserId(),
                new MemberRoleUpdateRequest(ProjectRole.MEMBER), manager.getEmail()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.PROJECT_LAST_MANAGER_DEMOTE);

        addMember(projectId, other, ProjectRole.MANAGER);
        projectService.updateMemberRole(projectId, manager.getUserId(), new MemberRoleUpdateRequest(ProjectRole.MEMBER), manager.getEmail());

        assertThat(projectMemberRepository.findByProjectProjectIdAndUserUserId(projectId, manager.getUserId()).orElseThrow().getRole())
                .isEqualTo(ProjectRole.MEMBER);
    }

    @Test
    void 초대_알림은_초대_ID를_가리키고_조회_시_최신_초대_상태를_함께_준다() {
        User manager = user("manager@test.com", 0);
        User invitee = user("invitee@test.com", 0);
        Long projectId = project(manager);
        ProjectInvitationResponse invitation = projectInvitationService.invite(projectId, manager.getUserId(), "invitee@test.com", ProjectRole.MANAGER);

        NotificationResponse notification = invitationNotifications(invitee).get(0);
        assertThat(notification.targetType()).isEqualTo(TargetType.INVITATION);
        assertThat(notification.targetId()).isEqualTo(invitation.projectInvitationId());
        assertThat(notification.invitation().invitationId()).isEqualTo(invitation.projectInvitationId());
        assertThat(notification.invitation().projectId()).isEqualTo(projectId);
        assertThat(notification.invitation().status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(notification.invitation().role()).isEqualTo(ProjectRole.MANAGER);

        projectInvitationService.respond(projectId, invitation.projectInvitationId(), invitee.getUserId(), InvitationAction.REJECT);

        assertThat(invitationNotifications(invitee).get(0).invitation().status()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    void 예전_형식_초대_알림은_그_프로젝트의_최신_초대로_맞춘다() {
        User manager = user("manager@test.com", 0);
        User invitee = user("invitee@test.com", 0);
        Long projectId = project(manager);
        ProjectInvitationResponse invitation = projectInvitationService.invite(projectId, manager.getUserId(), "invitee@test.com", null);
        jdbc.update("DELETE FROM notification");
        notificationService.create(invitee, NotificationType.PROJECT_INVITATION, "초대", "예전 알림", TargetType.PROJECT, projectId);

        NotificationResponse legacy = invitationNotifications(invitee).get(0);

        assertThat(legacy.targetType()).isEqualTo(TargetType.PROJECT);
        assertThat(legacy.invitation().invitationId()).isEqualTo(invitation.projectInvitationId());
    }

    @Test
    void 초대_알림이_아니면_invitation은_null이다() {
        User user = user("user@test.com", 0);
        notificationService.create(user, NotificationType.QNA_ANSWERED, "답변", "내용", TargetType.QNA, 1L);

        NotificationResponse notification = notificationService
                .getNotifications(user.getUserId(), false, null, null, null, PageRequest.of(0, 10)).getContent().get(0);

        assertThat(notification.invitation()).isNull();
    }

    @Test
    void 가입_전에_받은_초대는_가입할_때_연결되고_초대_알림이_온다() {
        User manager = user("manager@test.com", 0);
        Long projectId = project(manager);
        ProjectInvitationResponse invitation = projectInvitationService.invite(projectId, manager.getUserId(), "newbie@test.com", null);

        User newbie = ReflectionTestUtils.invokeMethod(customOAuth2UserService, "createUserWithPendingInvitations",
                "newbie@test.com", "newbie");

        List<NotificationResponse> notifications = invitationNotifications(newbie);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).invitation().invitationId()).isEqualTo(invitation.projectInvitationId());
    }

    private List<NotificationResponse> invitationNotifications(User user) {
        return notificationService.getNotifications(user.getUserId(), false, null, null, null, PageRequest.of(0, 10))
                .getContent().stream()
                .filter(n -> n.type() == NotificationType.PROJECT_INVITATION)
                .toList();
    }
}

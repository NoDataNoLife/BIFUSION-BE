package com.nodatanolife.bifusion_be.domain.project.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_invitation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_invitation_id")
    private Long projectInvitationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id")
    private User invitee;

    @Column(name = "invitee_email", length = 255)
    private String inviteeEmail;

    @Column(name = "role", length = 20)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InvitationStatus status;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "is_public")
    private Boolean isPublic;

    // 가입자 초대
    public static ProjectInvitation ofRegistered(Project project, User inviter, User invitee, String role) {
        ProjectInvitation invitation = new ProjectInvitation();
        invitation.project = project;
        invitation.inviter = inviter;
        invitation.invitee = invitee;
        invitation.inviteeEmail = invitee.getEmail();
        invitation.role = role;
        invitation.status = InvitationStatus.PENDING;
        invitation.lastSentAt = LocalDateTime.now();
        return invitation;
    }

    // 미가입자 초대
    public static ProjectInvitation ofUnregistered(Project project, User inviter, String email, String role) {
        ProjectInvitation invitation = new ProjectInvitation();
        invitation.project = project;
        invitation.inviter = inviter;
        invitation.invitee = null;
        invitation.inviteeEmail = email;
        invitation.role = role;
        invitation.status = InvitationStatus.PENDING;
        invitation.lastSentAt = LocalDateTime.now();
        return invitation;
    }

    public void resend() {
        this.lastSentAt = LocalDateTime.now();
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = InvitationStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }
}

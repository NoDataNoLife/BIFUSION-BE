package com.nodatanolife.bifusion_be.global.mail;

public interface MailService {
    void sendInvitationMail(String to, String projectName, String inviterName);
    void sendSignUpInvitationMail(String to, String projectName, String inviterName);
}
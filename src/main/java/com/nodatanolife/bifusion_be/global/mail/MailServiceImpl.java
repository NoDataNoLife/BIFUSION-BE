package com.nodatanolife.bifusion_be.global.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    // 기존 유저 초대 메일
    @Async
    @Override
    public void sendInvitationMail(String to, String projectName, String inviterName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[Bifusion] 프로젝트 초대");
        message.setText("""
                안녕하세요.

                %s님이 회원님을 [%s] 프로젝트에 초대했습니다.

                Bifusion에 로그인하여 초대를 수락하거나 거절할 수 있습니다.

                감사합니다.
                Bifusion 팀
                """.formatted(inviterName, projectName));
        send(message, to);
    }

    // 미가입 유저 초대 메일
    @Async
    @Override
    public void sendSignUpInvitationMail(String to, String projectName, String inviterName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[Bifusion] 프로젝트 초대");
        message.setText("""
                안녕하세요.

                %s님이 회원님을 [%s] 프로젝트에 초대했습니다.

                Bifusion에 가입하고 프로젝트에 참여해보세요.
                회원가입 후 해당 프로젝트에 참여할 수 있습니다.

                감사합니다.
                Bifusion 팀
                """.formatted(inviterName, projectName));
        send(message, to);
    }

    // 발송 실패 로그
    private void send(SimpleMailMessage message, String to) {
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("메일 발송 실패: to={}", to, e);
        }
    }
}
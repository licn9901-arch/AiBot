package com.deskpet.core.service;

import com.deskpet.core.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthMailService {

    private final JavaMailSender mailSender;
    private final AuthProperties authProperties;

    public void sendActivationEmail(String to, String username, String activationLink) {
        String subject = "DeskPet 账号激活";
        String content = "你好，" + username + "：\n\n"
            + "欢迎注册 DeskPet。请点击下面的链接激活账号：\n"
            + activationLink + "\n\n"
            + "如果这不是你的操作，请忽略此邮件。";
        send(to, subject, content);
    }

    public void sendPasswordResetEmail(String to, String username, String resetLink) {
        String subject = "DeskPet 重置密码";
        String content = "你好，" + username + "：\n\n"
            + "我们收到了你的重置密码请求。请点击下面的链接设置新密码：\n"
            + resetLink + "\n\n"
            + "如果这不是你的操作，请忽略此邮件。";
        send(to, subject, content);
    }

    private void send(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(authProperties.getMailFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}

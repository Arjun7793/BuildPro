package com.example.buildpro.service.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP through spring.mail.* - the default (app.mail.provider=smtp or unset). */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        if (message.from() != null && !message.from().isBlank()) {
            mail.setFrom(message.from());
        }
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.text());
        mailSender.send(mail);
    }
}

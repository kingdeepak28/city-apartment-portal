// Author: deepak.maheshwari

package com.societyportal.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends transactional email. When {@code app.mail.enabled=false} (the local/dev
 * default, since no real SMTP credentials are configured out of the box) it
 * just logs the message so the whole flow is still exercisable without a mail
 * server - flip the flag once real SMTP settings are supplied.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                         @Value("${app.mail.enabled}") boolean enabled,
                         @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
    }

    public boolean send(String to, String subject, String htmlBody) {
        if (!enabled) {
            log.info("[MAIL:DEV-MODE] to={} subject={}\n{}", to, subject, htmlBody);
            return true;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}

package com.otpservice.service.notification;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final String username;
    private final String password;
    private final String fromEmail;
    private final Session session;

    public EmailNotificationService() {
        Properties config = loadConfig();
        this.username = config.getProperty("email.username");
        this.password = config.getProperty("email.password");
        this.fromEmail = config.getProperty("email.from");
        this.session = Session.getInstance(config, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(EmailNotificationService.class.getClassLoader()
                    .getResourceAsStream("email.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load email configuration", e);
        }
    }

    public void sendCode(String toEmail, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);
            Transport.send(message);
            log.info("OTP sent via email to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

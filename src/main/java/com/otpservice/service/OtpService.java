package com.otpservice.service;

import com.otpservice.dao.OtpCodeDao;
import com.otpservice.dao.OtpConfigDao;
import com.otpservice.model.OtpCode;
import com.otpservice.model.OtpConfig;
import com.otpservice.model.enums.OtpStatus;
import com.otpservice.service.notification.*;
import com.otpservice.util.OtpGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    public enum Channel {EMAIL, SMS, TELEGRAM, FILE}

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao otpConfigDao;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final TelegramNotificationService telegramService;
    private final FileNotificationService fileService;

    public OtpService(OtpCodeDao otpCodeDao, OtpConfigDao otpConfigDao,
                      EmailNotificationService emailService,
                      SmsNotificationService smsService,
                      TelegramNotificationService telegramService,
                      FileNotificationService fileService) {
        this.otpCodeDao = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
        this.emailService = emailService;
        this.smsService = smsService;
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    public OtpCode generateOtp(long userId, String operationId, Channel channel, String destination) {
        OtpConfig config = otpConfigDao.findConfig();
        String code = OtpGenerator.generate(config.getCodeLength());

        OtpCode otpCode = new OtpCode();
        otpCode.setUserId(userId);
        otpCode.setOperationId(operationId);
        otpCode.setCode(code);
        otpCode.setStatus(OtpStatus.ACTIVE);
        otpCode.setCreatedAt(LocalDateTime.now());
        otpCode.setExpiresAt(LocalDateTime.now().plusSeconds(config.getTtlSeconds()));
        otpCodeDao.save(otpCode);

        sendViaChannel(channel, destination, code);
        log.info("OTP generated for userId={}, operationId={}, channel={}", userId, operationId, channel);
        return otpCode;
    }

    private void sendViaChannel(Channel channel, String destination, String code) {
        switch (channel) {
            case EMAIL -> emailService.sendCode(destination, code);
            case SMS -> smsService.sendCode(destination, code);
            case TELEGRAM -> telegramService.sendCode(destination, code);
            case FILE -> fileService.sendCode(destination, code);
        }
    }

    public boolean validateOtp(long userId, String operationId, String code) {
        boolean used = otpCodeDao.tryMarkUsed(userId, operationId, code);
        if (used) {
            log.info("OTP validated successfully. userId={}, operationId={}", userId, operationId);
        } else {
            log.warn("OTP validation failed: not found, expired, or already used. userId={}, operationId={}", userId, operationId);
        }
        return used;
    }

    public void updateConfig(int codeLength, int ttlSeconds) {
        otpConfigDao.updateConfig(codeLength, ttlSeconds);
    }
}

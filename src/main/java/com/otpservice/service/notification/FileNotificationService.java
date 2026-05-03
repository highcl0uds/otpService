package com.otpservice.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileNotificationService {
    private static final Logger log = LoggerFactory.getLogger(FileNotificationService.class);
    private static final String FILE_PATH = "otp-codes.txt";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendCode(String destination, String code) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH, true))) {
            writer.printf("[%s] destination=%s code=%s%n",
                    LocalDateTime.now().format(FMT), destination, code);
            log.info("OTP saved to file for {}", destination);
        } catch (IOException e) {
            log.error("Failed to write OTP to file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to write OTP to file", e);
        }
    }
}

package com.otpservice;

import com.otpservice.dao.*;
import com.otpservice.scheduler.OtpExpiryScheduler;
import com.otpservice.server.HttpServerConfig;
import com.otpservice.server.handlers.*;
import com.otpservice.service.*;
import com.otpservice.service.notification.*;
import com.otpservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            System.err.println("[FATAL] Startup failed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        Properties appProps = loadProperties("application.properties");

        DatabaseConnection.init(appProps);
        DatabaseConnection db = DatabaseConnection.getInstance();

        UserDao userDao = new UserDao(db);
        OtpConfigDao otpConfigDao = new OtpConfigDao(db);
        OtpCodeDao otpCodeDao = new OtpCodeDao(db);

        JwtUtil jwtUtil = new JwtUtil(
                appProps.getProperty("jwt.secret"),
                Long.parseLong(appProps.getProperty("jwt.expiration.hours", "1"))
        );

        EmailNotificationService emailService = new EmailNotificationService();
        SmsNotificationService smsService = new SmsNotificationService();
        TelegramNotificationService telegramService = new TelegramNotificationService();
        FileNotificationService fileService = new FileNotificationService();

        AuthService authService = new AuthService(userDao, jwtUtil);
        UserService userService = new UserService(userDao);
        OtpService otpService = new OtpService(otpCodeDao, otpConfigDao,
                emailService, smsService, telegramService, fileService);

        long schedulerInterval = Long.parseLong(appProps.getProperty("scheduler.interval.seconds", "60"));
        OtpExpiryScheduler scheduler = new OtpExpiryScheduler(otpCodeDao, schedulerInterval);
        scheduler.start();

        AuthHandler authHandler = new AuthHandler(authService);
        AdminHandler adminHandler = new AdminHandler(userService, otpService);
        OtpHandler otpHandler = new OtpHandler(otpService);

        int port = Integer.parseInt(appProps.getProperty("server.port", "8080"));
        HttpServerConfig serverConfig = new HttpServerConfig(port, authHandler, adminHandler, otpHandler, jwtUtil);
        serverConfig.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            scheduler.stop();
            serverConfig.stop();
        }));

        log.info("OTP Service started. Press Ctrl+C to stop.");
    }

    private static Properties loadProperties(String name) {
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) throw new RuntimeException("Resource not found: " + name);
            Properties props = new Properties();
            props.load(is);
            overrideFromEnv(props, "db.url",       "DB_URL");
            overrideFromEnv(props, "db.username",  "DB_USERNAME");
            overrideFromEnv(props, "db.password",  "DB_PASSWORD");
            overrideFromEnv(props, "server.port",  "SERVER_PORT");
            overrideFromEnv(props, "jwt.secret",   "JWT_SECRET");
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + name, e);
        }
    }

    private static void overrideFromEnv(Properties props, String key, String envVar) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) {
            props.setProperty(key, value);
        }
    }
}

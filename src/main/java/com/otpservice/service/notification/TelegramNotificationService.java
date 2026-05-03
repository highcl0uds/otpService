package com.otpservice.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TelegramNotificationService {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final String botToken;
    private final String chatId;
    private final String apiBaseUrl;
    private final HttpClient httpClient;

    public TelegramNotificationService() {
        Properties props = loadConfig();
        this.botToken = props.getProperty("telegram.bot.token");
        this.chatId = props.getProperty("telegram.chat.id");
        this.apiBaseUrl = props.getProperty("telegram.api.url");
        this.httpClient = HttpClient.newHttpClient();
    }

    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(TelegramNotificationService.class.getClassLoader()
                    .getResourceAsStream("telegram.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Telegram configuration", e);
        }
    }

    public void sendCode(String destination, String code) {
        String text = String.format("OTP code for %s: %s", destination, code);
        String url = String.format("%s%s/sendMessage?chat_id=%s&text=%s",
                apiBaseUrl, botToken, chatId, URLEncoder.encode(text, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("OTP sent via Telegram for {}", destination);
            } else {
                log.error("Telegram API error. Status: {}, Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Telegram API returned status " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Telegram send interrupted: {}", e.getMessage(), e);
            throw new RuntimeException("Telegram send interrupted", e);
        } catch (IOException e) {
            log.error("Telegram send IO error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}

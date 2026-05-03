package com.otpservice.server.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.model.OtpCode;
import com.otpservice.service.OtpService;
import com.otpservice.service.OtpService.Channel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class OtpHandler extends BaseHandler implements HttpHandler {

    private final OtpService otpService;

    public OtpHandler(OtpService otpService) {
        this.otpService = otpService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        log.info("OTP {} {}", method, path);

        try {
            if ("POST".equals(method) && path.endsWith("/generate")) {
                handleGenerate(exchange);
            } else if ("POST".equals(method) && path.endsWith("/validate")) {
                handleValidate(exchange);
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            log.error("Error in OtpHandler: {}", e.getMessage(), e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleGenerate(HttpExchange exchange) throws IOException {
        JsonNode body = parseBody(exchange);
        String operationId = body.path("operationId").asText(null);
        String channelStr = body.path("channel").asText(null);
        String destination = body.path("destination").asText("");

        if (operationId == null || operationId.isBlank()) {
            sendError(exchange, 400, "operationId is required");
            return;
        }
        if (channelStr == null) {
            sendError(exchange, 400, "channel is required (EMAIL, SMS, TELEGRAM, FILE)");
            return;
        }

        Channel channel;
        try {
            channel = Channel.valueOf(channelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "Invalid channel. Use EMAIL, SMS, TELEGRAM or FILE");
            return;
        }

        long userId = getUserId(exchange);
        try {
            OtpCode otp = otpService.generateOtp(userId, operationId, channel, destination);
            sendJson(exchange, 201, Map.of(
                    "message", "OTP generated and sent",
                    "operationId", operationId,
                    "expiresAt", otp.getExpiresAt().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to generate OTP: {}", e.getMessage(), e);
            sendError(exchange, 500, "Failed to generate OTP: " + e.getMessage());
        }
    }

    private void handleValidate(HttpExchange exchange) throws IOException {
        JsonNode body = parseBody(exchange);
        String operationId = body.path("operationId").asText(null);
        String code = body.path("code").asText(null);

        if (operationId == null || operationId.isBlank() || code == null || code.isBlank()) {
            sendError(exchange, 400, "operationId and code are required");
            return;
        }

        long userId = getUserId(exchange);
        boolean valid = otpService.validateOtp(userId, operationId, code);
        if (valid) {
            sendJson(exchange, 200, Map.of("valid", true, "message", "OTP accepted"));
        } else {
            sendJson(exchange, 400, Map.of("valid", false, "message", "OTP invalid or expired"));
        }
    }
}

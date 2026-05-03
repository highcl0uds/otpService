package com.otpservice.server.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.model.User;
import com.otpservice.service.OtpService;
import com.otpservice.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminHandler extends BaseHandler implements HttpHandler {

    private final UserService userService;
    private final OtpService otpService;

    public AdminHandler(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        log.info("ADMIN {} {}", method, path);

        try {
            if ("GET".equals(method) && path.endsWith("/users")) {
                handleGetUsers(exchange);
            } else if ("DELETE".equals(method) && path.contains("/users/")) {
                handleDeleteUser(exchange, path);
            } else if ("PUT".equals(method) && path.endsWith("/config")) {
                handleUpdateConfig(exchange);
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            log.error("Error in AdminHandler: {}", e.getMessage(), e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleGetUsers(HttpExchange exchange) throws IOException {
        List<User> users = userService.getAllNonAdminUsers();
        List<Map<String, Object>> result = users.stream()
                .map(u -> Map.<String, Object>of("id", u.getId(), "login", u.getLogin(), "role", u.getRole()))
                .collect(Collectors.toList());
        sendJson(exchange, 200, result);
    }

    private void handleDeleteUser(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        long userId;
        try {
            userId = Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid user id");
            return;
        }
        try {
            userService.deleteUser(userId);
            sendJson(exchange, 200, Map.of("message", "User deleted"));
        } catch (IllegalArgumentException e) {
            sendError(exchange, 404, e.getMessage());
        }
    }

    private void handleUpdateConfig(HttpExchange exchange) throws IOException {
        JsonNode body = parseBody(exchange);
        if (!body.has("codeLength") || !body.has("ttlSeconds")) {
            sendError(exchange, 400, "codeLength and ttlSeconds are required");
            return;
        }
        int codeLength = body.get("codeLength").asInt();
        int ttlSeconds = body.get("ttlSeconds").asInt();
        if (codeLength < 4 || codeLength > 12) {
            sendError(exchange, 400, "codeLength must be between 4 and 12");
            return;
        }
        if (ttlSeconds < 30) {
            sendError(exchange, 400, "ttlSeconds must be at least 30");
            return;
        }
        otpService.updateConfig(codeLength, ttlSeconds);
        sendJson(exchange, 200, Map.of("message", "Config updated", "codeLength", codeLength, "ttlSeconds", ttlSeconds));
    }
}

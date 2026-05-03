package com.otpservice.server.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.model.User;
import com.otpservice.model.enums.Role;
import com.otpservice.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class AuthHandler extends BaseHandler implements HttpHandler {

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        log.info("AUTH {} {}", method, path);

        try {
            if ("POST".equals(method) && path.endsWith("/register")) {
                handleRegister(exchange);
            } else if ("POST".equals(method) && path.endsWith("/login")) {
                handleLogin(exchange);
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            log.error("Error in AuthHandler: {}", e.getMessage(), e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        JsonNode body = parseBody(exchange);
        String login = body.path("login").asText(null);
        String password = body.path("password").asText(null);
        String roleStr = body.path("role").asText("USER");

        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            sendError(exchange, 400, "login and password are required");
            return;
        }

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "Invalid role. Use ADMIN or USER");
            return;
        }

        try {
            User user = authService.register(login, password, role);
            sendJson(exchange, 201, Map.of("id", user.getId(), "login", user.getLogin(), "role", user.getRole()));
        } catch (IllegalArgumentException e) {
            sendError(exchange, 409, e.getMessage());
        } catch (IllegalStateException e) {
            sendError(exchange, 409, e.getMessage());
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        JsonNode body = parseBody(exchange);
        String login = body.path("login").asText(null);
        String password = body.path("password").asText(null);

        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            sendError(exchange, 400, "login and password are required");
            return;
        }

        try {
            String token = authService.login(login, password);
            sendJson(exchange, 200, Map.of("token", token));
        } catch (IllegalArgumentException e) {
            sendError(exchange, 401, e.getMessage());
        }
    }
}

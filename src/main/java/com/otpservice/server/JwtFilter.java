package com.otpservice.server;

import com.otpservice.util.JwtUtil;
import com.otpservice.util.JsonUtil;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JwtFilter extends Filter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;
    private final String requiredRole;

    public JwtFilter(JwtUtil jwtUtil, String requiredRole) {
        this.jwtUtil = jwtUtil;
        this.requiredRole = requiredRole;
    }

    @Override
    public String description() {
        return "JWT authentication filter for role: " + requiredRole;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(exchange, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isValid(token)) {
            sendUnauthorized(exchange, "Invalid or expired token");
            return;
        }

        Claims claims = jwtUtil.validateAndExtract(token);
        String role = claims.get("role", String.class);
        long userId = Long.parseLong(claims.getSubject());

        if (!requiredRole.equals(role)) {
            sendForbidden(exchange, "Access denied: required role " + requiredRole);
            return;
        }

        exchange.setAttribute("userId", userId);
        exchange.setAttribute("role", role);
        log.debug("JWT authenticated userId={}, role={}", userId, role);
        chain.doFilter(exchange);
    }

    private void sendUnauthorized(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, 401, message);
    }

    private void sendForbidden(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, 403, message);
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] body = JsonUtil.MAPPER.writeValueAsBytes(Map.of("error", message));
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}

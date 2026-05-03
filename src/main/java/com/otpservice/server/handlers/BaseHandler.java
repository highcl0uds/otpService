package com.otpservice.server.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class BaseHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected JsonNode parseBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            if (bytes.length == 0) return JsonUtil.MAPPER.createObjectNode();
            return JsonUtil.MAPPER.readTree(bytes);
        }
    }

    protected void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String json = JsonUtil.MAPPER.writeValueAsString(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendJson(exchange, statusCode, Map.of("error", message));
    }

    protected String extractPathSegment(HttpExchange exchange, int index) {
        String[] parts = exchange.getRequestURI().getPath().split("/");
        if (index < parts.length) return parts[index];
        return null;
    }

    protected long getUserId(HttpExchange exchange) {
        return (long) exchange.getAttribute("userId");
    }

    protected String getRole(HttpExchange exchange) {
        return (String) exchange.getAttribute("role");
    }
}

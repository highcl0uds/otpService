package com.otpservice.server;

import com.otpservice.server.handlers.AdminHandler;
import com.otpservice.server.handlers.AuthHandler;
import com.otpservice.server.handlers.OtpHandler;
import com.otpservice.util.JwtUtil;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class HttpServerConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpServerConfig.class);

    private final int port;
    private final AuthHandler authHandler;
    private final AdminHandler adminHandler;
    private final OtpHandler otpHandler;
    private final JwtUtil jwtUtil;
    private HttpServer server;

    public HttpServerConfig(int port, AuthHandler authHandler, AdminHandler adminHandler,
                            OtpHandler otpHandler, JwtUtil jwtUtil) {
        this.port = port;
        this.authHandler = authHandler;
        this.adminHandler = adminHandler;
        this.otpHandler = otpHandler;
        this.jwtUtil = jwtUtil;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/auth/", authHandler);

        HttpContext adminContext = server.createContext("/api/admin/", adminHandler);
        adminContext.getFilters().add(new JwtFilter(jwtUtil, "ADMIN"));

        HttpContext otpContext = server.createContext("/api/otp/", otpHandler);
        otpContext.getFilters().add(new JwtFilter(jwtUtil, "USER"));

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        log.info("HTTP server started on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(2);
            log.info("HTTP server stopped");
        }
    }
}

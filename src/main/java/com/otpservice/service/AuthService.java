package com.otpservice.service;

import com.otpservice.dao.UserDao;
import com.otpservice.model.User;
import com.otpservice.model.enums.Role;
import com.otpservice.util.JwtUtil;
import com.otpservice.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final JwtUtil jwtUtil;

    public AuthService(UserDao userDao, JwtUtil jwtUtil) {
        this.userDao = userDao;
        this.jwtUtil = jwtUtil;
    }

    public User register(String login, String password, Role role) {
        if (userDao.findByLogin(login).isPresent()) {
            throw new IllegalArgumentException("User with login '" + login + "' already exists");
        }
        if (role == Role.ADMIN && userDao.existsByRole(Role.ADMIN)) {
            throw new IllegalStateException("Admin already exists");
        }
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(role);
        User saved = userDao.save(user);
        log.info("User registered: login={}, role={}", login, role);
        return saved;
    }

    public String login(String login, String password) {
        Optional<User> userOpt = userDao.findByLogin(login);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        User user = userOpt.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        log.info("User logged in: login={}", login);
        return token;
    }
}

package com.otpservice.dao;

import com.otpservice.model.User;
import com.otpservice.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    private final DatabaseConnection db;

    public UserDao(DatabaseConnection db) {
        this.db = db;
    }

    public User save(User user) {
        String sql = "INSERT INTO users(login, password_hash, role) VALUES(?, ?, ?) RETURNING id";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ResultSet rs = ps.executeQuery();
            rs.next();
            user.setId(rs.getLong("id"));
            log.debug("User saved: id={}, login={}", user.getId(), user.getLogin());
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public Optional<User> findByLogin(String login) {
        String sql = "SELECT id, login, password_hash, role FROM users WHERE login = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by login", e);
        }
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, login, password_hash, role FROM users WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id", e);
        }
    }

    public List<User> findAllNonAdmin() {
        String sql = "SELECT id, login, password_hash, role FROM users WHERE role != 'ADMIN'";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(mapRow(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find users", e);
        }
    }

    public boolean existsByRole(Role role) {
        String sql = "SELECT 1 FROM users WHERE role = ? LIMIT 1";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check role existence", e);
        }
    }

    public void deleteById(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            log.debug("User deleted: id={}", id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setLogin(rs.getString("login"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        return u;
    }
}

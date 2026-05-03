package com.otpservice.dao;

import com.otpservice.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class OtpConfigDao {
    private static final Logger log = LoggerFactory.getLogger(OtpConfigDao.class);

    private final DatabaseConnection db;

    public OtpConfigDao(DatabaseConnection db) {
        this.db = db;
    }

    public OtpConfig findConfig() {
        String sql = "SELECT id, code_length, ttl_seconds FROM otp_config LIMIT 1";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new OtpConfig(rs.getLong("id"), rs.getInt("code_length"), rs.getInt("ttl_seconds"));
            }
            throw new RuntimeException("OTP config not found");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load OTP config", e);
        }
    }

    public void updateConfig(int codeLength, int ttlSeconds) {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, codeLength);
            ps.setInt(2, ttlSeconds);
            ps.executeUpdate();
            log.info("OTP config updated: codeLength={}, ttlSeconds={}", codeLength, ttlSeconds);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update OTP config", e);
        }
    }
}

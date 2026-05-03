package com.otpservice.dao;

import com.otpservice.model.OtpCode;
import com.otpservice.model.enums.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class OtpCodeDao {
    private static final Logger log = LoggerFactory.getLogger(OtpCodeDao.class);

    private final DatabaseConnection db;

    public OtpCodeDao(DatabaseConnection db) {
        this.db = db;
    }

    public OtpCode save(OtpCode otpCode) {
        String sql = "INSERT INTO otp_codes(user_id, operation_id, code, status, created_at, expires_at) " +
                     "VALUES(?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, otpCode.getUserId());
            ps.setString(2, otpCode.getOperationId());
            ps.setString(3, otpCode.getCode());
            ps.setString(4, otpCode.getStatus().name());
            ps.setTimestamp(5, Timestamp.valueOf(otpCode.getCreatedAt()));
            ps.setTimestamp(6, Timestamp.valueOf(otpCode.getExpiresAt()));
            ResultSet rs = ps.executeQuery();
            rs.next();
            otpCode.setId(rs.getLong("id"));
            log.debug("OTP saved: id={}, operationId={}", otpCode.getId(), otpCode.getOperationId());
            return otpCode;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save OTP code", e);
        }
    }

    public boolean tryMarkUsed(long userId, String operationId, String code) {
        String sql = "UPDATE otp_codes SET status = 'USED' " +
                     "WHERE user_id = ? AND operation_id = ? AND code = ? " +
                     "AND status = 'ACTIVE' AND expires_at > NOW()";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, operationId);
            ps.setString(3, code);
            int updated = ps.executeUpdate();
            log.debug("tryMarkUsed: {} row(s) updated for operationId={}", updated, operationId);
            return updated == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate OTP code", e);
        }
    }

    public void updateStatus(long id, OtpStatus status) {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
            log.debug("OTP status updated: id={}, status={}", id, status);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update OTP status", e);
        }
    }

    public int markExpiredCodes() {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' " +
                     "WHERE status = 'ACTIVE' AND expires_at < NOW()";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int count = ps.executeUpdate();
            if (count > 0) {
                log.info("Marked {} OTP codes as EXPIRED", count);
            }
            return count;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark expired OTP codes", e);
        }
    }

    private OtpCode mapRow(ResultSet rs) throws SQLException {
        OtpCode o = new OtpCode();
        o.setId(rs.getLong("id"));
        o.setUserId(rs.getLong("user_id"));
        o.setOperationId(rs.getString("operation_id"));
        o.setCode(rs.getString("code"));
        o.setStatus(OtpStatus.valueOf(rs.getString("status")));
        o.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        o.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        return o;
    }
}

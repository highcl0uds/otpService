package com.otpservice.model;

import com.otpservice.model.enums.OtpStatus;

import java.time.LocalDateTime;

public class OtpCode {
    private long id;
    private long userId;
    private String operationId;
    private String code;
    private OtpStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public OtpCode() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public OtpStatus getStatus() { return status; }
    public void setStatus(OtpStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}

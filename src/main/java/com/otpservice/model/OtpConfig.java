package com.otpservice.model;

public class OtpConfig {
    private long id;
    private int codeLength;
    private int ttlSeconds;

    public OtpConfig() {}

    public OtpConfig(long id, int codeLength, int ttlSeconds) {
        this.id = id;
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }

    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }
}

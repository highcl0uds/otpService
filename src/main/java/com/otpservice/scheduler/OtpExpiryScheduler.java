package com.otpservice.scheduler;

import com.otpservice.dao.OtpCodeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpExpiryScheduler {
    private static final Logger log = LoggerFactory.getLogger(OtpExpiryScheduler.class);

    private final OtpCodeDao otpCodeDao;
    private final long intervalSeconds;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "otp-expiry-scheduler");
        t.setDaemon(true);
        return t;
    });

    public OtpExpiryScheduler(OtpCodeDao otpCodeDao, long intervalSeconds) {
        this.otpCodeDao = otpCodeDao;
        this.intervalSeconds = intervalSeconds;
    }

    public void start() {
        executor.scheduleAtFixedRate(() -> {
            try {
                otpCodeDao.markExpiredCodes();
            } catch (Exception e) {
                log.error("Error during OTP expiry check: {}", e.getMessage(), e);
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
        log.info("OTP expiry scheduler started (interval={}s)", intervalSeconds);
    }

    public void stop() {
        executor.shutdown();
        log.info("OTP expiry scheduler stopped");
    }
}

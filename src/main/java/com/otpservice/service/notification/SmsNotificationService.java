package com.otpservice.service.notification;

import org.jsmpp.bean.*;
import org.jsmpp.session.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmsNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsNotificationService() {
        Properties props = loadConfig();
        this.host = props.getProperty("smpp.host");
        this.port = Integer.parseInt(props.getProperty("smpp.port"));
        this.systemId = props.getProperty("smpp.system_id");
        this.password = props.getProperty("smpp.password");
        this.systemType = props.getProperty("smpp.system_type");
        this.sourceAddress = props.getProperty("smpp.source_addr");
    }

    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(SmsNotificationService.class.getClassLoader()
                    .getResourceAsStream("sms.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SMS configuration", e);
        }
    }

    public void sendCode(String destination, String code) {
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );
            session.connectAndBind(host, port, bindParameter);

            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    ("Your OTP code: " + code).getBytes(StandardCharsets.UTF_8)
            );
            log.info("OTP sent via SMS to {}", destination);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", destination, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        } finally {
            session.unbindAndClose();
        }
    }
}

package com.zktecodevice.device_monitoring;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zktecodevice.turnstile_connection.TurnstileHttpClient;

public class MonitoringTurnstile {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringTurnstile.class);

    private int connection;
    private String ip;
    private ScheduledExecutorService scheduler;

    public MonitoringTurnstile(String ip) throws Exception {
        this.ip = ip;
        connect();
    }

    private void connect() throws Exception {
        while (true) {
            String params = "protocol=TCP,ipaddress=" + ip + ",port=4370,timeout=10000,password=";
            connection = CatracaSDK.Plcommpro.INSTANCE.Connect(params);
            if (connection != 0) {
                logger.info("Successfully connected to the controller at: {} | Connection ID: {}", ip, connection);
                return;
            } else {
                logger.warn("Failed to connect to controller at: {}.", ip);
            }
            Thread.sleep(2000);
        }
    }

    public void disconnect() {
        if (connection != 0) {
            CatracaSDK.Plcommpro.INSTANCE.Disconnect(connection);
            logger.info("Connection to IP {} closed.", ip);
        }
    }

    private void reconnect() {
        try { 
            disconnect();
            connect();
        } catch (Exception e) {
            logger.error("Failed to connect to controller at: {}", ip, e);
        }
    }

    public void startMonitor() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitorEvent();
            } catch (Exception e) {
                logger.error("Error during monitoring: {}", e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void monitorEvent() {
        byte[] buffer = new byte[4096];

        int resultCode = CatracaSDK.Plcommpro.INSTANCE.GetRTLog(connection, buffer, buffer.length);
        if (resultCode > 0) {
            String data = new String(buffer).trim();
            String[] events = data.split("\r\n");
            for (String record : events) {
                processEvent(record);
            }
        } else if (resultCode == -2) {
            logger.warn("Lost connection to IP: {}. Attempting to reconnect...", ip);
            reconnect();
        } else {
            logger.error("Unknown error when monitoring events. Return code: {}", resultCode);
            reconnect();
        }
    }

    private void processEvent(String record) {
        String[] values = record.split(",");
        if (values.length < 7) {
            logger.warn("Invalid or incomplete record: {}", record);
            return;
        }
        
        try {
            int bit4 = Integer.parseInt(values[4].trim());
            if (bit4 == 255) {
                logger.debug("Ignored door/alarm status record: {}", record);
                return;
            }

            String eventTime = values[0].trim();
            String cardNumber = values[2].trim();
            int doorNumber = Integer.parseInt(values[3].trim());
            int eventCode = Integer.parseInt(values[4].trim());
            int passageDirection = Integer.parseInt(values[5].trim());

            logger.info("Event detected: Time: {} | Card: {} | Door: {} | Event Type: {} | Direction: {}",
                    eventTime, cardNumber, doorNumber, eventCode, getPassageDirection(passageDirection));

            String validationResult = TurnstileHttpClient.validateCard(ip, (long) doorNumber, cardNumber);

            if ("true".equalsIgnoreCase(validationResult)) {
                releasePassageWithMonitoring(doorNumber);
            } else {
                logger.warn("Invalid or unauthorized card: {}", cardNumber);
            }

        } catch (NumberFormatException e) {
            logger.error("Error processing record: {}", record, e);
        }
    }

    private static String getPassageDirection(int status) {
        switch (status) {
            case 0:
                return "Entry";
            case 1:
                return "Exit";
            case 2:
                return "None";
            default:
                return "Unknown";
        }
    }
    
    public void releasePassageWithMonitoring(int door) {
        int openTime = 5;
        
        boolean success = controlDevice(door, openTime);
        if (success) {
            logger.info("Turnstile released for door: {}", door);
            verifyPassage(door, openTime);
        } else {
            logger.error("Unexpected error. Code: {}");
        }
    }
    
    private boolean controlDevice(int door, int openTime) {
        int result = CatracaSDK.Plcommpro.INSTANCE.ControlDevice(connection, 1, door, 1, openTime, 0, "");
        if (result >= 0) {
            System.out.println("success");
            return true;
        } else {
            logger.error("Error releasing the turnstile: {}", result);
            reconnect();
            return false;
        }
    }
    
    private void verifyPassage(int door, int openTime) {
        long startTime = System.currentTimeMillis();
        boolean passageConfirmed = false;
        
        while (System.currentTimeMillis() - startTime < 10 * 1000) {
            try {
                byte[] buffer = new byte[4096];
                int resultCode = CatracaSDK.Plcommpro.INSTANCE.GetRTLog(connection, buffer, buffer.length);
                
                if (resultCode > 0) {
                    String data = new String(buffer).trim();
                    String[] events = data.split("\r\n");
                    
                    for (String record : events) {
                        String[] values = record.split(",");
                        
                        if (values.length < 7) {
                            logger.warn("Invalid or incomplete record: {}", record);
                            continue;
                        }
                        
                        String eventTime = values[0].trim();
                        String cardNumber = values[2].trim();
                        int passageDirection = Integer.parseInt(values[5].trim());
                        int doorNumber = Integer.parseInt(values[3].trim());
                        int eventCode = Integer.parseInt(values[4].trim());
                        
                        logger.info("Event detected: Time: {} | Card: {} | Door: {} | Event Type: {} | Direction: {}",
                                eventTime, cardNumber, doorNumber, eventCode, getPassageDirection(passageDirection));
                        
                        if (doorNumber == door && (eventCode == 200 || eventCode == 201)) {
                            TurnstileHttpClient.registerAccess(ip, (long) door);
                            logger.info("Confirmed passage through the turnstile for door {}", door);
                            passageConfirmed = true;
                            break;
                        }
                    }
                }
                
                if (passageConfirmed) {
                    break;
                }
            } catch (NumberFormatException e) {
                logger.error("Error when checking passage: {}", e.getMessage());
                break;
            }
        }
        
        if (!passageConfirmed) {
            TurnstileHttpClient.registerDesistance(ip, (long) door);
            logger.warn("Unregistered passage detected for door {}", door);
        }
    }
}

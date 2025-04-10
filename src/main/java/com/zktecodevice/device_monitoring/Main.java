package com.zktecodevice.device_monitoring;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        String[] ips = {"10.10.7.218", "10.10.7.219"};

        for (String ip : ips) {
            executorService.execute(() -> {
                try {
                    MonitoringTurnstile monitor = new MonitoringTurnstile(ip);
                    monitor.startMonitor();
                } catch (Exception e) {
                    System.err.println("Erro ao monitorar catraca com IP: " + ip);
                    e.printStackTrace();
                }
            });
        }

        executorService.shutdown();
    }
}

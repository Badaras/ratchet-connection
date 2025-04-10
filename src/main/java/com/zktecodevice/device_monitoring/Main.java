package com.zktecodevice.device_monitoring;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        String[] ips = {"99.99.9.999", "99.99.9.999"};

        for (String ip : ips) {
            executorService.execute(() -> {
                try {
                    MonitoramentoCatracas monitor = new MonitoramentoCatracas(ip);
                    monitor.iniciarMonitoramento();
                } catch (Exception e) {
                    System.err.println("Erro ao monitorar catraca com IP: " + ip);
                    e.printStackTrace();
                }
            });
        }

        executorService.shutdown();
    }
}

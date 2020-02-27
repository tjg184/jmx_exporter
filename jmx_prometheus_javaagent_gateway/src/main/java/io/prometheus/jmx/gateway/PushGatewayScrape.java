package io.prometheus.jmx.gateway;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PushGatewayScrape {

    private final PushGateway pushGateway;
    private final ScheduledExecutorService scheduledThreadPool;
    private final CollectorRegistry collectorRegistry;
    private int pushInterval = 5;

    public PushGatewayScrape(CollectorRegistry collectorRegistry, String host, int port) {
        this.pushGateway = new PushGateway(host + ":" + port);
        this.collectorRegistry = collectorRegistry;
        this.scheduledThreadPool = Executors.newScheduledThreadPool(1);
        sendOnInterval();
        pushAndShutdownOnTermination();
    }

    private void sendOnInterval() {
        scheduledThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                push();
            }
        }, 0, pushInterval, TimeUnit.SECONDS);
    }

    private void pushAndShutdownOnTermination() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                push();
                scheduledThreadPool.shutdown();
            }
        });
    }

    public void push() {
        try {
            pushGateway.pushAdd(collectorRegistry, System.getProperty("sun.java.command"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

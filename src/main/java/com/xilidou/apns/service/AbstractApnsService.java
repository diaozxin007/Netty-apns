package com.xilidou.apns.service;

import com.xilidou.apns.module.ApnsConfig;
import com.xilidou.apns.module.Payload;
import com.xilidou.apns.module.PushNotification;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractApnsService implements ApnsService {

    /**
     * expire in minutes
     */
    private static final int EXPIRE = 15 * 60 * 1000;

    private static final AtomicInteger IDS = new AtomicInteger(0);

    protected ExecutorService executorService;

    public AbstractApnsService(ApnsConfig config) {
        executorService = Executors.newFixedThreadPool(config.getPoolSize());
    }

    @Override
    public boolean sendNotification(String token, Payload payload) {
        PushNotification notification = new PushNotification();
        notification.setToken(token);
        notification.setPayload(payload);
        notification.setExpire(EXPIRE);
        notification.setId(IDS.incrementAndGet());
        return sendNotification(notification);
    }

    @Override
    public void shutdown() {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(6, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }
    }
}

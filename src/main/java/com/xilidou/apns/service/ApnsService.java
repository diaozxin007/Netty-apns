package com.xilidou.apns.service;


import com.xilidou.apns.module.Payload;
import com.xilidou.apns.module.PushNotification;

/**
 * Created by Zhengxin on 2017/4/10.
 */
public interface ApnsService {

    boolean sendNotification(String token, Payload payload);

    boolean sendNotification(PushNotification pushNotification);

    void shutdown();

}

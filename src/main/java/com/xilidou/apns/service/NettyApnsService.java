package com.xilidou.apns.service;

import com.xilidou.apns.module.ApnsConfig;
import com.xilidou.apns.module.Payload;
import com.xilidou.apns.module.PushNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Zhengxin on 2017/4/17.
 */
public class NettyApnsService extends AbstractApnsService {

    private static final Logger logger = LoggerFactory.getLogger(NettyApnsService.class);
    private static ApnsConnectionPool connectionPool;

    public NettyApnsService(ApnsConfig config) {
        super(config);
        connectionPool = new ApnsConnectionPool(config);
    }


    @Override
    public boolean sendNotification(String token, Payload payload) {
        PushNotification pushNotification = new PushNotification();
        pushNotification.setPayload(payload);
        pushNotification.setToken(token);
        return sendNotification(pushNotification);
    }

    @Override
    public boolean sendNotification(final PushNotification pushNotification) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                ApnsConnection apnsConnection ;
                try {
                    apnsConnection = connectionPool.getSource();
                    if(apnsConnection != null) {
                        apnsConnection.sendNotification(pushNotification);
                        if(apnsConnection.isBroken()){
                            apnsConnection.close();
                            connectionPool.returnBrokenSource(apnsConnection);
                        }else {
                            connectionPool.returnSource(apnsConnection);
                        }
                    }else{
                        logger.error("apnsconnection is null");
                    }
                } catch (Exception e) {
                    logger.error("NettyApnsService sendNotification error.",e);
                }
            }
        });
        return true;
    }

    @Override
    public void shutdown() {

    }
}

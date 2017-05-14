package com.xilidou.apns.service;

import com.xilidou.apns.module.ApnsConfig;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zhengxin on 2017/4/17.
 */
public class ApnsConnectionFactory extends BasePooledObjectFactory<ApnsConnection>{

    private static final Logger LOGGER = LoggerFactory.getLogger(ApnsConnectionFactory.class);

    private static final String HOST_DEVELOPMENT = "api.development.push.apple.com";

    private static final String HOST_PRODUCTION = "api.push.apple.com";

    private static final String ALGORITHM = "sunx509";

    private static final String KEY_STORE_TYPE = "PKCS12";

    private static final String ROOT_PATH = "";

    private static final int PORT = 2197;

    private AtomicInteger count = new AtomicInteger(0);

    private String host;
    private String apnsTopic;
    private KeyManagerFactory keyManagerFactory;
    private int timeout;
    private int retryTimes;

    private ApnsConfig config;

    public ApnsConnectionFactory(ApnsConfig config){
        this.config = config;
        this.timeout = config.getTimeout();
        this.retryTimes = config.getRetries();
        this.host = config.isDevEnv() ? HOST_DEVELOPMENT : HOST_PRODUCTION;
        this.apnsTopic = config.getApnsTopic();
        createKeyManagerFactory();
    }

    @Override
    public ApnsConnection create() throws Exception {
        return new ApnsConnection("conn-" + count.addAndGet(1),host,PORT,retryTimes,timeout,apnsTopic,keyManagerFactory);
    }

    @Override
    public PooledObject<ApnsConnection> wrap(ApnsConnection obj) {
        return new DefaultPooledObject<>(obj);
    }

    private void createKeyManagerFactory() {
        InputStream inputStream = null;
        try {
            char[] password = config.getPassword().toCharArray();
            inputStream = new FileInputStream(ROOT_PATH + File.separator + config.getKeyStorePath());
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            keyStore.load(inputStream, password);
            keyManagerFactory = KeyManagerFactory.getInstance(ALGORITHM);
            keyManagerFactory.init(keyStore, password);
        } catch (Exception e) {
            LOGGER.error("createKeyManagerFactory", e);
            throw new IllegalStateException("create key manager factory failed");
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.error("inputStream close error.", e);
                }
            }
        }
    }


}

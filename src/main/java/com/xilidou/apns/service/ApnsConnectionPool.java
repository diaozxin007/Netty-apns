package com.xilidou.apns.service;

import com.xilidou.apns.module.ApnsConfig;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

/**
 * Created by Zhengxin on 2017/4/21.
 */
public class ApnsConnectionPool{

    private static final Logger log = LoggerFactory.getLogger(ApnsConnectionPool.class);

    private GenericObjectPool<ApnsConnection> pool;

    ApnsConnectionPool(ApnsConfig apnsConfig){
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxIdle(apnsConfig.getMaxIdle());
        poolConfig.setMinIdle(apnsConfig.getMinIdle());
        poolConfig.setMaxTotal(apnsConfig.getMaxTotal());
        poolConfig.setMaxWaitMillis(-1);

        initPool(poolConfig,new ApnsConnectionFactory(apnsConfig));
    }

    public void initPool(final GenericObjectPoolConfig poolConfig, PooledObjectFactory<ApnsConnection> factory) {

        if (this.pool != null) {
            try {
                closeInternalPool();
            } catch (Exception e) {
            }
        }

        this.pool = new GenericObjectPool<>(factory, poolConfig);
    }

    protected void closeInternalPool() {
        try {
            pool.close();
        } catch (Exception e) {
            throw new RuntimeException("Could not destroy the pool", e);
        }
    }

    public ApnsConnection getSource(){
        try {
            return pool.borrowObject();
        }catch (NoSuchElementException e) {
           throw new RuntimeException("Could not get a resource since the pool is exhausted",e);
        }catch (Exception e){
            throw new RuntimeException("ould not get a resource from the pool",e);
        }
    }

    public void returnSource(final ApnsConnection source){
        if(source == null){
            return;
        }

        pool.returnObject(source);
    }

    public void returnBrokenSource(final ApnsConnection source){

        if(source == null){
            return;
        }

        try {
            pool.invalidateObject(source);
        } catch (Exception e) {
            log.error("invalidateObject error",e);
        }
    }

    public void destroy(){
        closeInternalPool();
    }
}

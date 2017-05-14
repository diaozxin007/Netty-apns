package com.xilidou.apns.module;

/**
 * Created by Zhengxin on 2017/4/10.
 */
public class ApnsConfig {

    private String name;

    private String keyStorePath;

    private String password;

    private boolean isDevEnv = false;

    private int poolSize = 32;

    private int cacheLength = 100;

    private int retries = 3;

    private int intervalTime = 1800000;

    private int timeout = 10000;

    private int maxIdle = 20;

    private int minIdle = 2;

    private int maxTotal = 20;

    private String apnsTopic = "";

    public ApnsConfig() {
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isDevEnv() {
        return this.isDevEnv;
    }

    public void setDevEnv(boolean isDevEnv) {
        this.isDevEnv = isDevEnv;
    }

    public int getPoolSize() {
        return this.poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getCacheLength() {
        return this.cacheLength;
    }

    public void setCacheLength(int cacheLength) {
        this.cacheLength = cacheLength;
    }

    public int getRetries() {
        return this.retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getName() {
        return this.name != null && !"".equals(this.name.trim()) ? this.name : (this.isDevEnv() ? "dev-env" : "product-env");
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIntervalTime() {
        return this.intervalTime;
    }

    public void setIntervalTime(int intervalTime) {
        this.intervalTime = intervalTime;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean getDevEnv() {
        return isDevEnv;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public String getApnsTopic() {
        return apnsTopic;
    }

    public void setApnsTopic(String apnsTopic) {
        this.apnsTopic = apnsTopic;
    }
}

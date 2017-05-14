package com.xilidou.apns.module;

import java.util.regex.Pattern;

public class PushNotification {

    public static final int PRIORITY_SENT_IMMEDIATELY = 10;

    public static final int PRIORITY_SENT_A_TIME = 5;

    private static final Pattern pattern = Pattern.compile("[ -]");

    private int id;

    private int expire;

    private String token;

    private Payload payload;

    private int priority = 10;

    public PushNotification() {
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExpire() {
        return this.expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = pattern.matcher(token).replaceAll("");
    }

    public Payload getPayload() {
        return this.payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public String toString() {
        String sb = "id=" +
                this.getId() +
                " token=" +
                this.getToken() +
                " payload=" +
                this.getPayload().toString();
        return sb;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}

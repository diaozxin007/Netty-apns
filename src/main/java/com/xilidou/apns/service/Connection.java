package com.xilidou.apns.service;

import java.io.Closeable;

/**
 * Created by Zhengxin on 2017/5/11.
 */
public interface Connection extends Closeable{

    void connect();

    boolean isConnect();

    void disConnect();

    boolean isBroken();

}

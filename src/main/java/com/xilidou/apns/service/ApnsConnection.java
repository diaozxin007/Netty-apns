package com.xilidou.apns.service;

import com.xilidou.apns.module.PushNotification;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Zhengxin on 2017/4/14.
 */
public class ApnsConnection implements Connection{

    private static final Logger log = LoggerFactory.getLogger(ApnsConnection.class);

    private static final int DEFULT_STREAM_ID = 3;

    private AtomicInteger streamId = new AtomicInteger(DEFULT_STREAM_ID);

    private String name;
    private String host;
    private int port;
    private int timeout;
    private int retryTimes;
    private KeyManagerFactory keyManagerFactory;
    private String apnsTopic;
    private EventLoopGroup eventLoopGroup;
    private Http2ClientInitializer http2ClientInitializer;
    private Channel channel;

    private boolean broken = false;

    public ApnsConnection(String name, String host, int port,int retryTimes, int timeout, String apnsTopic,KeyManagerFactory keyManagerFactory) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.retryTimes = retryTimes;
        this.keyManagerFactory = keyManagerFactory;
        this.apnsTopic = apnsTopic;
    }

    public boolean sendNotification(PushNotification pushNotification){
        connect();

        int retryCount = 0;
        int streamId = -1;
        boolean success = false;
        FullHttpRequest request = null;
        while (retryCount < retryTimes) {
            HttpResponseHandler responseHandler = http2ClientInitializer.responseHandler();

            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            if(StringUtils.isNotBlank(apnsTopic)){
                httpHeaders.add("apns-topic",apnsTopic);
            }

            if (request == null) {
                request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                        HttpMethod.POST, "https://" + host + "/3/device/" + pushNotification.getToken(),
                        Unpooled.copiedBuffer(pushNotification.getPayload().toString().getBytes()),new DefaultHttpHeaders(),httpHeaders);
                streamId = this.streamId.getAndAdd(2);
                responseHandler.put(streamId, channel.writeAndFlush(request), channel.newPromise());
            }

            Map<Integer, Integer> responses = responseHandler.awaitResponses(timeout, TimeUnit.MILLISECONDS);
            int code = responses.get(streamId);
            if (code == HttpResponseHandler.CODE_SUCCESS) {

                log.info(name + " send success token {} and number is {}", pushNotification.getToken(),streamId);
                success = true;
                break;
            } else if (code == HttpResponseHandler.CODE_READ_TIMEOUT || code == HttpResponseHandler.CODE_WRITE_TIMEOUT) {
                log.info(name + " send timeout  token {} and number is {}", pushNotification.getToken(),streamId);
                retryCount++;
            } else if (code == HttpResponseHandler.CODE_READ_FAILED || code == HttpResponseHandler.CODE_WRITE_FAILED) {
                log.info(name + " send error  token {} and number is {}", pushNotification.getToken(),streamId);
                request = null;
                retryCount++;
                try {
                    broken = true;
                    channel.close().sync();
                } catch (InterruptedException e) {
                    log.error("close channel", e);
                }
            }

        }

        if (!success) {
            log.error("push error token {}",pushNotification.getToken());
        }

        return success;

    }

    public void connect(){
        if(!isConnect()){
            try {
                initHttp2ClientHandler();
            } catch (Exception e) {
                broken = true;
                log.error(name + "init http2Clinet error",e);
                throw new RuntimeException(name + "init http2Clinet error",e);
            }
        }
    }

    public boolean isConnect(){
        return channel != null && channel.isActive() && channel.isOpen() && channel.isRegistered()
                && channel.isWritable() && http2ClientInitializer != null;

    }

    public void disConnect(){
        if(!isConnect()) {
            if(channel != null) {
                channel.flush();
                channel.close().syncUninterruptibly();
            }
            http2ClientInitializer = null;
            if(eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public void close() {
        disConnect();
    }

    private void initHttp2ClientHandler() throws Exception
    {
        eventLoopGroup = new NioEventLoopGroup(4);
        Http2ClientInitializer http2ClientInitializer = new Http2ClientInitializer(name,createSslContext(),Integer.MAX_VALUE);

        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE,true)
                .remoteAddress(host,port)
                .handler(http2ClientInitializer);

        log.debug(name + "connect to {}:{}",host,port);

        channel = bootstrap.connect().syncUninterruptibly().channel();

        Http2SettingsHandler http2SettingsHandler = http2ClientInitializer.settingsHandler();
        http2SettingsHandler.awaitSettings(5, TimeUnit.SECONDS);
        this.http2ClientInitializer = http2ClientInitializer;
    }

    private SslContext createSslContext() throws SSLException {
        SslProvider provider = SslProvider.JDK;
        return SslContextBuilder.forClient()
                .sslProvider(provider)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .keyManager(keyManagerFactory)
                .applicationProtocolConfig(ApplicationProtocolConfig.DISABLED)
                .build();
    }

    public boolean isBroken(){
        return broken;
    }

}

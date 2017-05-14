package com.xilidou.apns.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Process {@link FullHttpResponse} translated from HTTP/2 frames
 */
class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    static final int CODE_SUCCESS = 0;

    static final int CODE_WRITE_TIMEOUT = 1;

    static final int CODE_WRITE_FAILED = 2;

    static final int CODE_READ_TIMEOUT = 3;

    static final int CODE_READ_FAILED = 4;

    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);

    private String name;

    private ConcurrentHashMap<Integer, Entry<ChannelFuture, ChannelPromise>> streamIdPromiseMap;

    HttpResponseHandler(String name) {
        this.name = name;
        streamIdPromiseMap = new ConcurrentHashMap<>();
    }

    /**
     * Create an association between an anticipated response stream id and a {@link
     * ChannelPromise}
     *
     * @param streamId    The stream for which a response is expected
     * @param writeFuture A future that represent the request write operation
     * @param promise     The promise object that will be used to wait/notify events
     * @return The previous object associated with {@code streamId}
     * @see HttpResponseHandler#awaitResponses(long, TimeUnit)
     */
    Entry<ChannelFuture, ChannelPromise> put(int streamId, ChannelFuture writeFuture, ChannelPromise promise) {
        dumpStreamIdPromiseMap("put");
        return streamIdPromiseMap.put(streamId, new SimpleEntry<>(writeFuture, promise));
    }

    private void dumpStreamIdPromiseMap(String tag) {
        if (!logger.isDebugEnabled()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(tag).append(" ").append("streamIdMap: ");
        for (Integer streamId : streamIdPromiseMap.keySet()) {
            builder.append(streamId).append(" ");
        }
        debug(builder.toString());
    }

    /**
     * Wait (sequentially) for a time duration for each anticipated response
     *
     * @param timeout Value of time to wait for each response
     * @param unit    Units associated with {@code timeout}
     * @see HttpResponseHandler#put(int, ChannelFuture,
     * ChannelPromise)
     */
    Map<Integer, Integer> awaitResponses(long timeout, TimeUnit unit) {
        dumpStreamIdPromiseMap("awaitResponses");
        HashMap<Integer, Integer> responses = new HashMap<>();
        Iterator<Entry<Integer, Entry<ChannelFuture, ChannelPromise>>> itr = streamIdPromiseMap.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<Integer, Entry<ChannelFuture, ChannelPromise>> entry = itr.next();
            ChannelFuture writeFuture = entry.getValue().getKey();
            if (!writeFuture.awaitUninterruptibly(timeout, unit)) {
                responses.put(entry.getKey(), CODE_WRITE_TIMEOUT);
                debug("write id " + entry.getKey() + " timeout");
                continue;
            }

            if (!writeFuture.isSuccess()) {
                responses.put(entry.getKey(), CODE_WRITE_FAILED);
                itr.remove();
                debug("write id " + entry.getKey() + " failed");
                continue;
            }

            ChannelPromise promise = entry.getValue().getValue();

            if (!promise.awaitUninterruptibly(timeout, unit)) {
                debug("read id " + entry.getKey() + " timeout");
                responses.put(entry.getKey(), CODE_READ_TIMEOUT);
                continue;
            }

            if (!promise.isSuccess()) {
                responses.put(entry.getKey(), CODE_READ_FAILED);
                itr.remove();
                debug("read id " + entry.getKey() + " failed");
                continue;
            }

            responses.put(entry.getKey(), 0);

            debug("stream id: " + entry.getKey() + " received");
            itr.remove();
        }
        return responses;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        dumpStreamIdPromiseMap("channelRead0");
        Integer streamId = msg.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
        if (streamId == null) {
            error("HttpResponseHandler unexpected message received: " + msg);
            return;
        }

        Entry<ChannelFuture, ChannelPromise> entry = streamIdPromiseMap.get(streamId);
        if (entry == null) {
            error("Message received for unknown stream id " + streamId);
        } else {
            // Do stuff with the message (for now just print it)
            ByteBuf content = msg.content();
            if (content.isReadable()) {
                int contentLength = content.readableBytes();
                byte[] arr = new byte[contentLength];
                content.readBytes(arr);
                logger.debug(new String(arr, 0, contentLength, CharsetUtil.UTF_8));
            }

            entry.getValue().setSuccess();
        }
    }

    private void debug(String message) {
        logger.debug(name + " " + message);
    }

    private void error(String message) {
        logger.error(name + " " + message);
    }

    private void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
}

/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: Request.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.impl;

import org.smartboot.http.common.DecodeState;
import org.smartboot.http.common.Reset;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.enums.HttpTypeEnum;
import org.smartboot.http.common.exception.HttpException;
import org.smartboot.http.common.io.BodyInputStream;
import org.smartboot.http.common.io.ChunkedInputStream;
import org.smartboot.http.common.io.PostInputStream;
import org.smartboot.http.common.io.ReadListener;
import org.smartboot.http.common.logging.Logger;
import org.smartboot.http.common.logging.LoggerFactory;
import org.smartboot.http.common.utils.NumberUtils;
import org.smartboot.http.server.Http2ServerHandler;
import org.smartboot.http.server.HttpServerConfiguration;
import org.smartboot.http.server.ServerHandler;
import org.smartboot.http.server.WebSocketHandler;
import org.smartboot.socket.timer.HashedWheelTimer;
import org.smartboot.socket.timer.TimerTask;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2018/8/31
 */
public final class Request extends CommonRequest implements Reset {
    private static final Logger LOGGER = LoggerFactory.getLogger(Request.class);
    private static final int INIT_CONTENT_LENGTH = -2;
    private static final int NONE_CONTENT_LENGTH = -1;


    private final DecoderUnit decodeState = new DecoderUnit();
    private ReadListener listener;

    /**
     * 消息类型
     */
    private HttpTypeEnum type = null;

    private HttpRequestImpl httpRequest;
    private Http2Session http2Request;
    private WebSocketRequestImpl webSocketRequest;
    private ServerHandler serverHandler;

    /**
     * 剩余可读字节数
     */
    private long remainingThreshold;

    /**
     * 最近一次IO时间
     */
    private long latestIo;
    private TimerTask httpIdleTask;
    private TimerTask wsIdleTask;
    private BodyInputStream inputStream;
    private Map<String, String> trailerFields;

    void cancelHttpIdleTask() {
        synchronized (this) {
            if (httpIdleTask != null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("cancel http idle monitor, request:{}", this);
                }
                httpIdleTask.cancel();
                httpIdleTask = null;
            }
        }
    }

    void cancelWsIdleTask() {
        synchronized (this) {
            if (wsIdleTask != null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("cancel websocket idle monitor, request:{}", this);
                }
                wsIdleTask.cancel();
                wsIdleTask = null;
            }
        }
    }

    Request(HttpServerConfiguration configuration, AioSession aioSession) {
        super(aioSession, configuration);
        this.remainingThreshold = configuration.getMaxRequestSize();
        if (configuration.getWsIdleTimeout() > 0) {
            wsIdleTask = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                LOGGER.debug("check wsIdle monitor");
                if (System.currentTimeMillis() - latestIo > configuration.getWsIdleTimeout() && webSocketRequest != null) {
                    LOGGER.debug("close ws connection by idle monitor");
                    try {
                        aioSession.close();
                    } finally {
                        cancelWsIdleTask();
                        cancelHttpIdleTask();
                    }
                }
            }, configuration.getWsIdleTimeout(), TimeUnit.MILLISECONDS);
        }
        if (configuration.getHttpIdleTimeout() > 0) {
            httpIdleTask = HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
                LOGGER.debug("check httpIdle monitor");
                if (System.currentTimeMillis() - latestIo > configuration.getHttpIdleTimeout() && webSocketRequest == null) {
                    LOGGER.debug("close http connection by idle monitor");
                    try {
                        aioSession.close();
                    } finally {
                        cancelHttpIdleTask();
                        cancelWsIdleTask();
                    }
                }
            }, configuration.getHttpIdleTimeout(), TimeUnit.MILLISECONDS);
        }
    }

    public void setInputStream(BodyInputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public BodyInputStream getInputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        //如果一个消息即存在传输译码（Transfer-Encoding）头域并且也 Content-Length 头域，后者会被忽略。
        if (HeaderValueEnum.CHUNKED.getName().equalsIgnoreCase(getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName()))) {
            inputStream = new ChunkedInputStream(aioSession, remainingThreshold, stringStringMap -> this.trailerFields = stringStringMap);
        } else {
            long contentLength = getContentLength();
            if (contentLength > 0) {
                inputStream = new PostInputStream(aioSession, contentLength);
            } else {
                inputStream = BodyInputStream.EMPTY_INPUT_STREAM;
            }
        }
        return inputStream;
    }

    public AioSession getAioSession() {
        return aioSession;
    }

    void decodeSize(int size) {
        remainingThreshold -= size;
        if (remainingThreshold < 0) {
            throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    public HttpTypeEnum getRequestType() {
        if (type != null) {
            return type;
        }
        if (serverHandler instanceof WebSocketHandler) {
            type = HttpTypeEnum.WEBSOCKET;
        } else if (serverHandler instanceof Http2ServerHandler) {
            type = HttpTypeEnum.HTTP_2;
        } else {
            type = HttpTypeEnum.HTTP;
        }
        return type;
    }

    public void setType(HttpTypeEnum type) {
        this.type = type;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }


    public long getContentLength() {
        if (contentLength > INIT_CONTENT_LENGTH) {
            return contentLength;
        }
        //不包含content-length,则为：-1
        contentLength = NumberUtils.toLong(getHeader(HeaderNameEnum.CONTENT_LENGTH.getName()), NONE_CONTENT_LENGTH);
        if (contentLength >= remainingThreshold) {
            throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        return contentLength;
    }

    /**
     * Returns the Internet Protocol (IP) address of the client
     * or last proxy that sent the request.
     * For HTTP servlets, same as the value of the
     * CGI variable <code>REMOTE_ADDR</code>.
     *
     * @return a <code>String</code> containing the
     * IP address of the client that sent the request
     */

    public String getRemoteAddr() {
        if (remoteAddr != null) {
            return remoteAddr;
        }
        try {
            InetSocketAddress remote = aioSession.getRemoteAddress();
            InetAddress address = remote.getAddress();
            if (address == null) {
                remoteAddr = remote.getHostString();
            } else {
                remoteAddr = address.getHostAddress();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return remoteAddr;
    }


    public InetSocketAddress getRemoteAddress() {
        try {
            return aioSession.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public InetSocketAddress getLocalAddress() {
        try {
            return aioSession.getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the fully qualified name of the client
     * or the last proxy that sent the request.
     * If the engine cannot or chooses not to resolve the hostname
     * (to improve performance), this method returns the dotted-string form of
     * the IP address. For HTTP servlets, same as the value of the CGI variable
     * <code>REMOTE_HOST</code>.
     *
     * @return a <code>String</code> containing the fully
     * qualified name of the client
     */

    public String getRemoteHost() {
        if (remoteHost != null) {
            return remoteHost;
        }
        try {
            remoteHost = aioSession.getRemoteAddress().getHostString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return remoteHost;
    }

    public HttpRequestImpl newHttpRequest() {
        if (httpRequest == null) {
            httpRequest = new HttpRequestImpl(this);
            cancelWsIdleTask();
        }
        return httpRequest;
    }

    public Http2Session newHttp2Session() {
        if (http2Request == null) {
            http2Request = new Http2Session(this);
            cancelWsIdleTask();
        }
        return http2Request;
    }

    public WebSocketRequestImpl newWebsocketRequest() {
        if (webSocketRequest == null) {
            webSocketRequest = new WebSocketRequestImpl(this);
            cancelHttpIdleTask();
        }
        return webSocketRequest;
    }

    public Map<String, String> getTrailerFields() {
        return trailerFields;
    }

    public void setLatestIo(long latestIo) {
        this.latestIo = latestIo;
    }


    public DecoderUnit getDecodeState() {
        return decodeState;
    }

    public void reset() {
        super.reset();
        remainingThreshold = configuration.getMaxRequestSize();
        method = null;
        httpRequest = null;
        webSocketRequest = null;
        decodeState.setState(DecodeState.STATE_METHOD);
        trailerFields = null;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
    }
}

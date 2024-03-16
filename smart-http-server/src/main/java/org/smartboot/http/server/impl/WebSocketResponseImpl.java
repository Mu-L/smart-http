/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: WebSocketResponseImpl.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.impl;

import org.smartboot.http.common.codec.websocket.CloseReason;
import org.smartboot.http.common.logging.Logger;
import org.smartboot.http.common.logging.LoggerFactory;
import org.smartboot.http.common.utils.WebSocketUtil;
import org.smartboot.http.server.WebSocketResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public class WebSocketResponseImpl extends AbstractResponse implements WebSocketResponse {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketResponseImpl.class);
    private boolean closed;

    public WebSocketResponseImpl(WebSocketRequestImpl webSocketRequest) {
        init(webSocketRequest, new WebSocketOutputStream(webSocketRequest, this));
    }

    @Override
    public void sendTextMessage(String text) {
        if (LOGGER.isInfoEnabled()) LOGGER.info("发送字符串消息: " + text);
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        try {
            WebSocketUtil.send(getOutputStream(), WebSocketUtil.OPCODE_TEXT, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendBinaryMessage(byte[] bytes) {
        if (LOGGER.isInfoEnabled()) LOGGER.info("发送二进制消息: " + Arrays.toString(bytes));
        try {
            WebSocketUtil.send(getOutputStream(), WebSocketUtil.OPCODE_BINARY, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendBinaryMessage(byte[] bytes, int offset, int length) {
        try {
            WebSocketUtil.send(getOutputStream(), WebSocketUtil.OPCODE_BINARY, bytes, offset, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pong(byte[] bytes) {
        try {
            WebSocketUtil.send(getOutputStream(), WebSocketUtil.OPCODE_PONG, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        close(CloseReason.NORMAL_CLOSURE, "");
    }

    @Override
    public void close(int code, String reason) {
        if (closed) {
            return;
        }
        closed = true;
        try {
            WebSocketUtil.send(getOutputStream(), WebSocketUtil.OPCODE_CLOSE, new CloseReason(code, reason).toBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            super.close();
        }
    }

    @Override
    public void ping(byte[] bytes) {
        try {
            WebSocketUtil.send(getOutputStream(), WebSocketUtil.OPCODE_PING, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void flush() {
        try {
            getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

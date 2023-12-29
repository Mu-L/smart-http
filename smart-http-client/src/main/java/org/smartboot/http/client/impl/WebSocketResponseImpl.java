/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: Response.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client.impl;

import org.smartboot.http.client.AbstractResponse;
import org.smartboot.http.client.WebSocketResponse;
import org.smartboot.http.common.codec.websocket.BasicFrameDecoder;
import org.smartboot.http.common.codec.websocket.Decoder;
import org.smartboot.http.common.codec.websocket.WebSocket;
import org.smartboot.http.common.enums.DecodePartEnum;
import org.smartboot.http.common.utils.SmartDecoder;
import org.smartboot.http.common.utils.WebSocketUtil;
import org.smartboot.socket.transport.AioSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/2
 */
public class WebSocketResponseImpl extends AbstractResponse implements WebSocket, WebSocketResponse {
    public final static Decoder basicFrameDecoder = new BasicFrameDecoder();
    private Decoder decoder = basicFrameDecoder;
    private final ByteArrayOutputStream payload = new ByteArrayOutputStream();
    private boolean frameFinalFlag;
    private boolean frameMasked;
    private int frameRsv;
    private int frameOpcode;

    /**
     * payload长度
     */
    private long payloadLength;

    private byte[] maskingKey;
    private SmartDecoder payloadDecoder;
    private CompletableFuture<AbstractResponse> future;

    public WebSocketResponseImpl(AioSession session, CompletableFuture future) {
        super(session, future);
        this.future = future;
    }


    public void reset() {
        setDecodePartEnum(DecodePartEnum.BODY);
        if (frameOpcode != WebSocketUtil.OPCODE_CONTINUE) {
            payload.reset();
        }
        decoder = basicFrameDecoder;
    }

    public boolean isFrameFinalFlag() {
        return frameFinalFlag;
    }

    public void setFrameFinalFlag(boolean frameFinalFlag) {
        this.frameFinalFlag = frameFinalFlag;
    }

    public boolean isFrameMasked() {
        return frameMasked;
    }

    public void setFrameMasked(boolean frameMasked) {
        this.frameMasked = frameMasked;
    }

    public int getFrameRsv() {
        return frameRsv;
    }

    public void setFrameRsv(int frameRsv) {
        this.frameRsv = frameRsv;
    }

    public int getFrameOpcode() {
        return frameOpcode;
    }

    public void setFrameOpcode(int frameOpcode) {
        this.frameOpcode = frameOpcode;
    }

    public byte[] getPayload() {
        return payload.toByteArray();
    }

    public long getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(long payloadLength) {
        this.payloadLength = payloadLength;
    }

    public byte[] getMaskingKey() {
        return maskingKey;
    }

    public void setMaskingKey(byte[] maskingKey) {
        this.maskingKey = maskingKey;
    }

    public void setPayload(byte[] payload) {
        try {
            this.payload.write(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public SmartDecoder getPayloadDecoder() {
        return payloadDecoder;
    }

    @Override
    public void setPayloadDecoder(SmartDecoder payloadDecoder) {
        this.payloadDecoder = payloadDecoder;
    }

    @Override
    public CompletableFuture<AbstractResponse> getFuture() {
        return future;
    }

    public void setFuture(CompletableFuture future) {
        this.future = future;
    }
}

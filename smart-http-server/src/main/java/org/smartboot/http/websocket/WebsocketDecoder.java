/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: WebsocketProtocol.java
 * Date: 2018-02-11
 * Author: sandao
 */

package org.smartboot.http.websocket;

import org.smartboot.socket.Protocol;
import org.smartboot.http.HttpEntity;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/11
 */
public class WebsocketDecoder implements Protocol<HttpEntity> {
    private static final AttachKey<DataFraming> ENTITY = AttachKey.valueOf("entity");

    private void unmask(DataFraming framing, ByteBuffer payLoadBuffer) {
        int i = payLoadBuffer.position();
        int end = payLoadBuffer.limit();
        int intMask = ((framing.getMaskingKey()[0] & 0xFF) << 24)
                | ((framing.getMaskingKey()[1] & 0xFF) << 16)
                | ((framing.getMaskingKey()[2] & 0xFF) << 8)
                | (framing.getMaskingKey()[3] & 0xFF);
        for (; i + 3 < end; i += 4) {
            int unmasked = payLoadBuffer.getInt(i) ^ intMask;
            payLoadBuffer.putInt(i, unmasked);
        }
        for (; i < end; i++) {
            payLoadBuffer.put(i, (byte) (payLoadBuffer.get(i) ^ framing.getMaskingKey()[i % 4]));
        }
    }

    @Override
    public HttpEntity decode(ByteBuffer buffer, AioSession<HttpEntity> session, boolean eof) {
        Attachment attachment = session.getAttachment();
        if (attachment == null) {
            throw new RuntimeException("decodeUnit is null");
        }
        DataFraming dataFraming = attachment.get(ENTITY);
        while (buffer.hasRemaining()) {
            switch (dataFraming.getState()) {
                case READING_FIRST: {
                    byte b = buffer.get();
                    dataFraming.setFrameFinalFlag((b & 0x80) != 0);
                    dataFraming.setFrameRsv((b & 0x70) >> 4);
                    dataFraming.setFrameOpcode(b & 0x0F);
                    dataFraming.setState(State.READING_SECOND);
                    break;
                }
                case READING_SECOND: {
                    byte b = buffer.get();
                    dataFraming.setFrameMasked((b & 0x80) != 0);
                    dataFraming.setFramePayloadLen1(b & 0x7F);

                    dataFraming.setState(State.READING_SIZE);
                    break;
                }
                case READING_SIZE: {
                    if (dataFraming.getFramePayloadLen1() == 126) {
                        if (buffer.remaining() < 2) {
                            return null;
                        }
                        int length = buffer.getShort() & 0xFFFF;//无符号整数
                        if (length < 126) {
                            throw new RuntimeException("");
                        }
                        dataFraming.setFramePayloadLength(length);
                    } else if (dataFraming.getFramePayloadLen1() == 127) {
                        if (buffer.remaining() < 8) {
                            return null;
                        }
                        long length = buffer.getLong();
                        if (length < 65536) {
                            throw new RuntimeException("");
                        }
                        dataFraming.setFramePayloadLength(length);
                    } else {
                        dataFraming.setFramePayloadLength(dataFraming.getFramePayloadLen1());
                    }

                    if (dataFraming.getFramePayloadLength() > Integer.MAX_VALUE) {
                        throw new RuntimeException("too long");
                    }
                    //todo
//                    decodeUnit.setFormBodyDecoder(new FixedLengthFrameDecoder((int) dataFraming.getFramePayloadLength()));
                    dataFraming.setState(State.MASKING_KEY);
                    break;
                }
                case MASKING_KEY: {
                    if (dataFraming.isFrameMasked()) {
                        if (buffer.remaining() < 4) {
                            return null;
                        }
                        byte[] maskingKey = new byte[4];
                        buffer.get(maskingKey);
                        dataFraming.setMaskingKey(maskingKey);
                    }
                    dataFraming.setState(State.PAYLOAD);
                    break;
                }
                case PAYLOAD: {
                    //todo
//                    if (decodeUnit.getFormBodyDecoder().decode(buffer)) {
//                        ByteBuffer payloadBuffer = decodeUnit.getFormBodyDecoder().getBuffer();
//
//                        //掩码处理
//                        if (dataFraming.isFrameMasked()) {
//                            unmask(dataFraming, payloadBuffer);
//                        }
//                        dataFraming.setData(payloadBuffer);
//                    }
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public ByteBuffer encode(HttpEntity msg, AioSession<HttpEntity> session) {
        return null;
    }
}

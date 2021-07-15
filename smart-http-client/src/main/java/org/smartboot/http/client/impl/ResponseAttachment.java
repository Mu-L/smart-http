/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: RequestAttachment.java
 * Date: 2021-05-26
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client.impl;

import org.smartboot.http.client.decode.HeaderDecoder;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/5/26
 */
public class ResponseAttachment {
    private final Response response;

    private HeaderDecoder decoder;

    private ByteBuffer byteBuffer;

    public ResponseAttachment(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public HeaderDecoder getDecoder() {
        return decoder;
    }

    public void setDecoder(HeaderDecoder decoder) {
        this.decoder = decoder;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }
}

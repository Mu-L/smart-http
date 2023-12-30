/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpHeaderDecoder.java
 * Date: 2021-07-15
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client.decode;

import org.smartboot.http.client.AbstractResponse;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.exception.HttpException;
import org.smartboot.http.common.utils.Constant;
import org.smartboot.http.common.utils.StringUtils;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2020/3/30
 */
class HttpHeaderDecoder implements HeaderDecoder {

    private final HeaderValueDecoder headerValueDecoder = new HeaderValueDecoder();

    @Override
    public HeaderDecoder decode(ByteBuffer byteBuffer, AioSession aioSession, AbstractResponse request) {
        if (byteBuffer.remaining() < 2) {
            return this;
        }
        //header解码结束
        if (byteBuffer.get(byteBuffer.position()) == Constant.CR) {
            if (byteBuffer.get(byteBuffer.position() + 1) != Constant.LF) {
                throw new HttpException(HttpStatus.BAD_REQUEST);
            }
            byteBuffer.position(byteBuffer.position() + 2);
//            return decoder.decode(byteBuffer, aioSession, request);
            return HeaderDecoder.BODY_READY_DECODER;
        }
        //Header name解码
        int length = StringUtils.scanUntilAndTrim(byteBuffer, Constant.COLON);
        if (length < 0) {
            return this;
        }
        String name = StringUtils.convertToString(byteBuffer, byteBuffer.position() - length - 1, length, StringUtils.String_CACHE_HEADER_NAME);
        request.setHeaderTemp(name);
        return headerValueDecoder.decode(byteBuffer, aioSession, request);
    }

    /**
     * Value值解码
     */
    class HeaderValueDecoder implements HeaderDecoder {
        @Override
        public HeaderDecoder decode(ByteBuffer byteBuffer, AioSession aioSession, AbstractResponse request) {
            int length = StringUtils.scanUntilAndTrim(byteBuffer, Constant.LF);
            if (length == -1) {
                return this;
            }
            request.setHeadValue(StringUtils.convertToString(byteBuffer, byteBuffer.position() - 1 - length, length - 1));
            return HttpHeaderDecoder.this.decode(byteBuffer, aioSession, request);
        }
    }
}

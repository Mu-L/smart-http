/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: DefaultHttpLifecycle.java
 * Date: 2021-07-15
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client.impl;

import org.smartboot.http.client.AbstractResponse;
import org.smartboot.http.client.ResponseHandler;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.exception.HttpException;
import org.smartboot.http.common.utils.Constant;
import org.smartboot.http.common.utils.FixedLengthFrameDecoder;
import org.smartboot.http.common.utils.GzipUtils;
import org.smartboot.http.common.utils.SmartDecoder;
import org.smartboot.http.common.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/7/15
 */
public class DefaultHttpResponseHandler extends ResponseHandler {
    private static final ResponseHandler DEFAULT_HANDLER = new ResponseHandler() {
        @Override
        public void onBodyStream(ByteBuffer buffer, AbstractResponse request) {

        }
    };
    private ResponseHandler responseHandler;

    @Override
    public final void onBodyStream(ByteBuffer buffer, AbstractResponse baseHttpResponse) {
        if (responseHandler != null) {
            responseHandler.onBodyStream(buffer, baseHttpResponse);
            return;
        }
        String transferEncoding = baseHttpResponse.getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName());
        if (StringUtils.equals(transferEncoding, HeaderValueEnum.CHUNKED.getName())) {
            responseHandler = new ChunkedHttpLifecycle();
        } else if (baseHttpResponse.getContentLength() > 0) {
            responseHandler = new ContentLengthHttpLifecycle();
        } else {
            responseHandler = DEFAULT_HANDLER;
        }
        onBodyStream(buffer, baseHttpResponse);
    }

    /**
     * @author 三刀（zhengjunweimail@163.com）
     * @version V1.0 , 2021/7/12
     */
    public static class ChunkedHttpLifecycle extends ResponseHandler {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private PART part = PART.CHUNK_LENGTH;
        private SmartDecoder chunkedDecoder;

        @Override
        public void onBodyStream(ByteBuffer buffer, AbstractResponse response) {
            switch (part) {
                case CHUNK_LENGTH:
                    decodeChunkedLength(buffer, response);
                    break;
                case CHUNK_CONTENT:
                    decodeChunkedContent(buffer, response);
                    break;
                case CHUNK_END:
                    decodeChunkedEnd(buffer, response);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        private void decodeChunkedContent(ByteBuffer buffer, AbstractResponse response) {
            if (chunkedDecoder.decode(buffer)) {
                try {
                    body.write(chunkedDecoder.getBuffer().array());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                part = PART.CHUNK_END;
                onBodyStream(buffer, response);
            }
        }

        private void decodeChunkedEnd(ByteBuffer buffer, AbstractResponse response) {
            if (buffer.remaining() < 2) {
                return;
            }
            if (buffer.get() == Constant.CR && buffer.get() == Constant.LF) {
                part = PART.CHUNK_LENGTH;
                onBodyStream(buffer, response);
            } else {
                throw new IllegalStateException();
            }
        }

        private void decodeChunkedLength(ByteBuffer buffer, AbstractResponse response) {
            int length = StringUtils.scanUntilAndTrim(buffer, Constant.LF);
            if (length < 0) {
                return;
            }
            if (length == 1) {
                finishDecode((HttpResponseImpl) response);
                return;
            }
            String contentLength = StringUtils.convertToString(buffer, buffer.position() - length - 1, length - 1);
            int chunkedLength = Integer.parseInt(contentLength, 16);
            if (chunkedLength != 0) {
                part = PART.CHUNK_CONTENT;
                chunkedDecoder = new FixedLengthFrameDecoder(chunkedLength);
            }
            onBodyStream(buffer, response);
        }

        public void finishDecode(HttpResponseImpl response) {
            if (StringUtils.equals(HeaderValueEnum.GZIP.getName(), response.getHeader(HeaderNameEnum.CONTENT_ENCODING.getName()))) {
                response.setBody(GzipUtils.uncompressToString(body.toByteArray()));
            } else {
                response.setBody(body.toString());
            }
            callback(response);
        }

        enum PART {
            CHUNK_LENGTH, CHUNK_CONTENT, CHUNK_END
        }
    }

    /**
     * @author 三刀（zhengjunweimail@163.com）
     * @version V1.0 , 2021/7/12
     */
    public static class ContentLengthHttpLifecycle extends ResponseHandler {
        private SmartDecoder smartDecoder;

        @Override
        public void onBodyStream(ByteBuffer buffer, AbstractResponse abstractResponse) {
            HttpResponseImpl response = (HttpResponseImpl) abstractResponse;
            if (smartDecoder == null) {
                int bodyLength = response.getContentLength();
                if (bodyLength > Constant.maxBodySize) {
                    throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
                }
                smartDecoder = new FixedLengthFrameDecoder(response.getContentLength());
            }
            if (smartDecoder.decode(buffer)) {
                response.setBody(new String(smartDecoder.getBuffer().array(), Charset.forName(response.getCharacterEncoding())));
                callback(abstractResponse);
            }
        }
    }

    private static void callback(AbstractResponse response) {
        response.getFuture().complete(response);
    }
}

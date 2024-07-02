/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: AbstractOutputStream.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client.impl;

import org.smartboot.http.common.BufferOutputStream;
import org.smartboot.http.common.Cookie;
import org.smartboot.http.common.HeaderValue;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.common.enums.HttpProtocolEnum;
import org.smartboot.http.common.utils.Constant;
import org.smartboot.socket.transport.AioSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
abstract class AbstractOutputStream extends BufferOutputStream {

    protected final AbstractRequest request;

    public AbstractOutputStream(AbstractRequest request, AioSession aioSession) {
        super(aioSession);
        this.request = request;
    }


    /**
     * 输出Http消息头
     *
     * @throws IOException
     */
    protected final void writeHeader(HeaderWriteSource source) throws IOException {
        if (committed) {
            return;
        }

        //输出http状态行、contentType,contentLength、Transfer-Encoding、server等信息
        String headLine = request.getMethod() + " " + request.getUri() + " " + request.getProtocol() + "\r\n";
        writeBuffer.write(getBytes(headLine));
        //转换Cookie
        convertCookieToHeader(request);

        if (request.getContentType() != null) {
            writeBuffer.write(getHeaderNameBytes(HeaderNameEnum.CONTENT_TYPE.getName()));
            writeBuffer.write(getBytes(String.valueOf(request.getContentType())));
            writeBuffer.write(Constant.CRLF_BYTES);
        }

        if (request.getContentLength() >= 0) {
            writeBuffer.write(getHeaderNameBytes(HeaderNameEnum.CONTENT_LENGTH.getName()));
            writeBuffer.write(getBytes(String.valueOf(request.getContentLength())));
            writeBuffer.write(Constant.CRLF_BYTES);
        } else if (chunkedSupport && source == HeaderWriteSource.WRITE) {
            request.addHeader(HeaderNameEnum.TRANSFER_ENCODING.getName(), HeaderValueEnum.CHUNKED.getName());
        }

        //输出Header部分
        if (request.getHeaders() != null) {
            for (Map.Entry<String, HeaderValue> entry : request.getHeaders().entrySet()) {
                HeaderValue headerValue = entry.getValue();
                while (headerValue != null) {
                    writeBuffer.write(getHeaderNameBytes(entry.getKey()));
                    writeBuffer.write(getBytes(headerValue.getValue()));
                    writeBuffer.write(Constant.CRLF_BYTES);
                    headerValue = headerValue.getNextValue();
                }
            }
        }
        writeBuffer.write(Constant.CRLF_BYTES);
        committed = true;
    }


    private void convertCookieToHeader(AbstractRequest request) {
        List<Cookie> cookies = request.getCookies();
        if (cookies == null || cookies.size() == 0) {
            return;
        }
        cookies.forEach(cookie -> request.addHeader(HeaderNameEnum.SET_COOKIE.getName(), cookie.toString()));

    }
}

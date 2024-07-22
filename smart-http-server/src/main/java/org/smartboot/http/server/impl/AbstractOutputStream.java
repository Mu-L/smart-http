/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: AbstractOutputStream.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.impl;

import org.smartboot.http.common.BufferOutputStream;
import org.smartboot.http.common.Cookie;
import org.smartboot.http.common.HeaderValue;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.utils.Constant;
import org.smartboot.http.server.HttpServerConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
abstract class AbstractOutputStream extends BufferOutputStream {


    protected static String SERVER_LINE = null;
    protected final AbstractResponse response;
    protected final AbstractRequest request;
    protected final HttpServerConfiguration configuration;

    public AbstractOutputStream(AbstractRequest httpRequest, AbstractResponse response) {
        super(httpRequest.request.getAioSession());
        this.response = response;
        this.request = httpRequest;
        this.configuration = httpRequest.request.getConfiguration();
        if (SERVER_LINE == null) {
            SERVER_LINE = HeaderNameEnum.SERVER.getName() + Constant.COLON_CHAR + configuration.serverName() + Constant.CRLF;
        }
    }

    /**
     * 输出Http消息头
     */
    protected void writeHeader(HeaderWriteSource source) throws IOException {
        if (committed) {
            return;
        }

        //转换Cookie
        convertCookieToHeader();

        boolean hasHeader = hasHeader();
        //输出http状态行、contentType,contentLength、Transfer-Encoding、server等信息
        writeBuffer.write(getHeadPart(hasHeader));
        if (hasHeader) {
            //输出Header部分
            writeHeaders();
        }
        committed = true;
    }

    protected abstract byte[] getHeadPart(boolean hasHeader);

    private void convertCookieToHeader() {
        List<Cookie> cookies = response.getCookies();
        if (cookies.size() > 0) {
            cookies.forEach(cookie -> response.addHeader(HeaderNameEnum.SET_COOKIE.getName(), cookie.toString()));
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        if (configuration.getWsIdleTimeout() > 0 || configuration.getHttpIdleTimeout() > 0) {
            request.request.setLatestIo(System.currentTimeMillis());
        }
    }

    protected boolean hasHeader() {
        return response.getHeaders().size() > 0;
    }

    private void writeHeaders() throws IOException {
        for (Map.Entry<String, HeaderValue> entry : response.getHeaders().entrySet()) {
            HeaderValue headerValue = entry.getValue();
            while (headerValue != null) {
                writeBuffer.write(getHeaderNameBytes(entry.getKey()));
                writeBuffer.write(getBytes(headerValue.getValue()));
                writeBuffer.write(Constant.CRLF_BYTES);
                headerValue = headerValue.getNextValue();
            }
        }
        writeBuffer.write(Constant.CRLF_BYTES);
    }
}

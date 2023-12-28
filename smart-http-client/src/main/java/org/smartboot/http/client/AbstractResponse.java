/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: AbstractResponse.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client;

import org.smartboot.http.common.HeaderValue;
import org.smartboot.http.common.Reset;
import org.smartboot.http.common.enums.DecodePartEnum;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.utils.NumberUtils;
import org.smartboot.http.common.utils.StringUtils;
import org.smartboot.socket.transport.AioSession;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractResponse implements Response, Reset {

    private static final int INIT_CONTENT_LENGTH = -2;
    private static final int NONE_CONTENT_LENGTH = -1;
    /**
     * Http请求头
     */
    private final List<HeaderValue> headers = new ArrayList<>(8);
    private final AioSession session;
    private String headerTemp;
    private int headerSize = 0;
    /**
     * Http协议版本
     */
    private String protocol;
    private String contentType;
    private int contentLength = INIT_CONTENT_LENGTH;

    /**
     * http 响应码
     */
    private int status;
    /**
     * 响应码描述
     */
    private String reasonPhrase;
    private String encoding;
    private DecodePartEnum decodePartEnum = DecodePartEnum.HEADER_FINISH;
    private ResponseHandler responseHandler;
    private final CompletableFuture<AbstractResponse> future;

    public AbstractResponse(AioSession session, CompletableFuture<AbstractResponse> future) {
        this.session = session;
        this.future = future;
    }

    public AioSession getSession() {
        return session;
    }

    public final String getHeader(String headName) {
        for (int i = 0; i < headerSize; i++) {
            HeaderValue headerValue = headers.get(i);
            if (headerValue.getName().equalsIgnoreCase(headName)) {
                return headerValue.getValue();
            }
        }
        return null;
    }

    public final Collection<String> getHeaders(String name) {
        List<String> value = new ArrayList<>(4);
        for (int i = 0; i < headerSize; i++) {
            HeaderValue headerValue = headers.get(i);
            if (headerValue.getName().equalsIgnoreCase(name)) {
                value.add(headerValue.getValue());
            }
        }
        return value;
    }

    public final Collection<String> getHeaderNames() {
        Set<String> nameSet = new HashSet<>();
        for (int i = 0; i < headerSize; i++) {
            nameSet.add(headers.get(i).getName());
        }
        return nameSet;
    }

    public final void setHeadValue(String value) {
        setHeader(headerTemp, value);
    }

    public final void setHeader(String headerName, String value) {
        if (headerSize < headers.size()) {
            HeaderValue headerValue = headers.get(headerSize);
            headerValue.setName(headerName);
            headerValue.setValue(value);
        } else {
            headers.add(new HeaderValue(headerName, value));
        }
        headerSize++;
    }

    public DecodePartEnum getDecodePartEnum() {
        return decodePartEnum;
    }

    public void setDecodePartEnum(DecodePartEnum decodePartEnum) {
        this.decodePartEnum = decodePartEnum;
    }

    public final String getProtocol() {
        return protocol;
    }

    public final void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setHeaderTemp(String headerTemp) {
        this.headerTemp = headerTemp;
    }

    public final String getContentType() {
        if (contentType != null) {
            return contentType;
        }
        contentType = getHeader(HeaderNameEnum.CONTENT_TYPE.getName());
        return contentType;
    }

    public final int getContentLength() {
        if (contentLength > INIT_CONTENT_LENGTH) {
            return contentLength;
        }
        //不包含content-length,则为：-1
        contentLength = NumberUtils.toInt(getHeader(HeaderNameEnum.CONTENT_LENGTH.getName()), NONE_CONTENT_LENGTH);
        return contentLength;
    }

    public final String getCharacterEncoding() {
        if (encoding != null) {
            return encoding;
        }
        String contentType = getContentType();
        String charset = StringUtils.substringAfter(contentType, "charset=");
        if (StringUtils.isNotBlank(charset)) {
            this.encoding = Charset.forName(charset).name();
        } else {
            this.encoding = "utf8";
        }
        return this.encoding;
    }


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    public ResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    public CompletableFuture<AbstractResponse> getFuture() {
        return future;
    }
}

/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpResponse.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server;

import org.smartboot.http.common.BufferOutputStream;
import org.smartboot.http.common.Cookie;
import org.smartboot.http.common.enums.HttpStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Http消息响应接口
 *
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public interface HttpResponse {

    /**
     * 响应消息输出流
     *
     * @return
     */
    BufferOutputStream getOutputStream();

    /**
     * 获取Http响应状态
     *
     * @return
     */
    int getHttpStatus();

    /**
     * 设置Http响应状态,若不设置默认{@link HttpStatus#OK}
     *
     * @param httpStatus
     */
    void setHttpStatus(HttpStatus httpStatus);

    /**
     * 设置Http响应状态,若不设置默认{@link HttpStatus#OK}
     *
     * @param httpStatus
     */
    void setHttpStatus(int httpStatus, String reasonPhrase);

    /**
     * 获取Http响应描述
     *
     * @return
     */
    String getReasonPhrase();

    /**
     * Sets a response header with the given name and value. If the header had
     * already been set, the new value overwrites the previous one. The
     * <code>containsHeader</code> method can be used to test for the presence
     * of a header before setting its value.
     *
     * @param name  the name of the header
     * @param value the header value If it contains octet string, it should be
     *              encoded according to RFC 2047
     *              (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #addHeader
     */
    public void setHeader(String name, String value);

    /**
     * Adds a response header with the given name and value. This method allows
     * response headers to have multiple values.
     *
     * @param name  the name of the header
     * @param value the additional header value If it contains octet string, it
     *              should be encoded according to RFC 2047
     *              (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #setHeader
     */
    public void addHeader(String name, String value);

    String getHeader(String name);

    /**
     * Return a Collection of all the header values associated with the
     * specified header name.
     *
     * @param name Header name to look up
     * @return The values for the specified header. These are the raw values so
     * if multiple values are specified in a single header that will be
     * returned as a single header value.
     * @since Servlet 3.0
     */
    public Collection<String> getHeaders(String name);

    /**
     * Get the header names set for this HTTP response.
     *
     * @return The header names set for this HTTP response.
     * @since Servlet 3.0
     */
    public Collection<String> getHeaderNames();

    void setContentLength(long contentLength);

    long getContentLength();

    void setContentType(String contentType);

    String getContentType();

    void write(byte[] data) throws IOException;

    public void close();

    /**
     * 添加Cookie信息
     *
     * @param cookie
     */
    void addCookie(Cookie cookie);

    /**
     * Sets the supplier of trailer headers.
     *
     * <p>
     * The trailer header field value is defined as a comma-separated list (see Section 3.2.2 and Section 4.1.2 of RFC
     * 7230).
     * </p>
     *
     * <p>
     * The supplier will be called within the scope of whatever thread/call causes the response content to be completed.
     * Typically this will be any thread calling close() on the output stream or writer.
     * </p>
     *
     * <p>
     * The trailers that run afoul of the provisions of section 4.1.2 of RFC 7230 are ignored.
     * </p>
     *
     * <p>
     * The RFC requires the name of every key that is to be in the supplied Map is included in the comma separated list that
     * is the value of the "Trailer" response header. The application is responsible for ensuring this requirement is met.
     * Failure to do so may lead to interoperability failures.
     * </p>
     *
     * @implSpec The default implementation is a no-op.
     *
     * @param supplier the supplier of trailer headers
     *
     * @exception IllegalStateException if it is invoked after the response has has been committed, or the trailer is not
     * supported in the request, for instance, the underlying protocol is HTTP 1.0, or the response is not in chunked
     * encoding in HTTP 1.1.
     *
     * @since Servlet 4.0
     */
    default public void setTrailerFields(Supplier<Map<String, String>> supplier) {
    }

    /**
     * Gets the supplier of trailer headers.
     *
     * @implSpec The default implememtation return null.
     *
     * @return <code>Supplier</code> of trailer headers
     *
     * @since Servlet 4.0
     */
    default public Supplier<Map<String, String>> getTrailerFields() {
        return null;
    }

}

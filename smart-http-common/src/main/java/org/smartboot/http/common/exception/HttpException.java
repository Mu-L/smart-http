/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpException.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.common.exception;

import org.smartboot.http.common.enums.HttpStatus;

/**
 * HTTP异常
 *
 * @author 三刀
 * @version V1.0 , 2018/6/3
 */
public class HttpException extends RuntimeException {
    private final HttpStatus httpStatus;

    public HttpException(HttpStatus httpStatus) {
        super(httpStatus.getReasonPhrase());
        this.httpStatus = httpStatus;
    }

    public HttpException(int httpStatusCode, String desc) {
        this(new HttpStatus(httpStatusCode, desc));
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

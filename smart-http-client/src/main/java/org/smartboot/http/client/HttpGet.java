/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpGet.java
 * Date: 2021-02-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client;

import org.smartboot.http.common.enums.HttpMethodEnum;

import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/3
 */
public class HttpGet extends HttpRestWrapper {

    HttpGet(HttpRestImpl rest) {
        super(rest);
        rest.setMethod(HttpMethodEnum.GET.getMethod());
    }


    @Override
    public HttpGet onSuccess(Consumer<HttpResponse> consumer) {
        super.onSuccess(consumer);
        return this;
    }

    @Override
    public HttpGet onFailure(Consumer<Throwable> consumer) {
        super.onFailure(consumer);
        return this;
    }

    @Override
    public Header<HttpGet> header() {
        return new HeaderWrapper<>(this, rest.header());
    }
}

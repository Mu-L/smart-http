/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: AbstractDecoder.java
 * Date: 2021-06-10
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.decode;

import org.smartboot.http.common.utils.ByteTree;
import org.smartboot.http.common.utils.Constant;
import org.smartboot.http.server.HttpServerConfiguration;
import org.smartboot.http.server.impl.Request;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/10
 */
public abstract class AbstractDecoder implements Decoder {
    protected static final ByteTree.EndMatcher CR_END_MATCHER = endByte -> endByte == Constant.CR;
    protected static final ByteTree.EndMatcher SP_END_MATCHER = endByte -> endByte == Constant.SP;
    private final HttpServerConfiguration configuration;

    private final AbstractDecoder wafDecoder;

    public AbstractDecoder(HttpServerConfiguration configuration) {
        this(configuration, null);
    }

    public AbstractDecoder(HttpServerConfiguration configuration, AbstractDecoder wafDecoder) {
        this.configuration = configuration;
        this.wafDecoder = wafDecoder;
    }

    @Override
    public final Decoder decode(ByteBuffer byteBuffer, Request request) {
        Decoder decoder = decode0(byteBuffer, request);
        if (wafDecoder == null || decoder == this) {
            return decoder;
        }
        Decoder waf = wafDecoder.decode0(byteBuffer, request);
        return waf == null ? decoder : waf;
    }

    protected abstract Decoder decode0(ByteBuffer byteBuffer, Request request);

    public HttpServerConfiguration getConfiguration() {
        return configuration;
    }
}

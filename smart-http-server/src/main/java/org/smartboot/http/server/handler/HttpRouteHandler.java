/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: RouteHandle.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.handler;

import org.smartboot.http.common.enums.HttpProtocolEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.logging.Logger;
import org.smartboot.http.common.logging.LoggerFactory;
import org.smartboot.http.common.utils.AntPathMatcher;
import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.http.server.HttpServerHandler;
import org.smartboot.http.server.ServerHandler;
import org.smartboot.http.server.impl.Request;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version V1.0 , 2018/3/24
 */
public final class HttpRouteHandler extends HttpServerHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(HttpRouteHandler.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    /**
     * 默认404
     */
    private final HttpServerHandler defaultHandler;
    private final Map<String, HttpServerHandler> handlerMap = new ConcurrentHashMap<>();

    public HttpRouteHandler() {
        this(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                response.setHttpStatus(HttpStatus.NOT_FOUND);
            }
        });
    }

    public HttpRouteHandler(HttpServerHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void onHeaderComplete(Request request) throws IOException {
        ServerHandler httpServerHandler = matchHandler(request.getRequestURI());
        //注册 URI 与 Handler 的映射关系
        request.getConfiguration().getUriByteTree().addNode(request.getUri(), httpServerHandler);
        //更新本次请求的实际 Handler
        request.setServerHandler(httpServerHandler);
        httpServerHandler.onHeaderComplete(request);
    }

    @Override
    public void onClose(Request request) {
        LOGGER.warn("connection is closed before route match.");
        defaultHandler.onClose(request);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, CompletableFuture<Object> completableFuture) throws Throwable {
        if (request.getProtocol() == HttpProtocolEnum.HTTP_2) {
            ServerHandler httpServerHandler = matchHandler(request.getRequestURI());
            httpServerHandler.handle(request, response, completableFuture);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * 配置URL路由
     *
     * @param urlPattern  url匹配
     * @param httpHandler 处理handler
     * @return
     */
    public HttpRouteHandler route(String urlPattern, HttpServerHandler httpHandler) {
        handlerMap.put(urlPattern, httpHandler);
        return this;
    }

    private HttpServerHandler matchHandler(String uri) {
        if (uri == null) {
            return defaultHandler;
        }
        HttpServerHandler httpHandler = handlerMap.get(uri);
        if (httpHandler == null) {
            for (Map.Entry<String, HttpServerHandler> entity : handlerMap.entrySet()) {
                if (PATH_MATCHER.match(entity.getKey(), uri)) {
                    httpHandler = entity.getValue();
                    break;
                }
            }
            if (httpHandler == null) {
                httpHandler = defaultHandler;
            }
        }
        return httpHandler;
    }
}

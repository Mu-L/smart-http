/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpBootstrap.java
 * Date: 2018-01-28
 * Author: sandao
 */

package org.smartboot.http.demo;

import org.smartboot.http.server.HttpBootstrap;
import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.http.server.HttpServerHandler;
import org.smartboot.http.server.handler.HttpRouteHandler;

import java.io.IOException;

public class Bootstrap {
    static byte[] body = "Hello, World!".getBytes();

    public static void main(String[] args) {
        HttpRouteHandler routeHandle = new HttpRouteHandler();
        routeHandle.route("/plaintext", new HttpServerHandler() {


            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                response.setContentLength(body.length);
                response.setContentType("text/plain; charset=UTF-8");
                response.write(body);
            }
        });
        int cpuNum = Runtime.getRuntime().availableProcessors();
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        HttpBootstrap bootstrap = new HttpBootstrap();
        bootstrap.configuration().threadNum(cpuNum).debug(false).headerLimiter(0).readBufferSize(1024 * 4).writeBufferSize(1024 * 4).readMemoryPool(16384 * 1024 * 4).writeMemoryPool(10 * 1024 * 1024 * cpuNum, cpuNum);
        bootstrap.httpHandler(routeHandle).setPort(8080).start();
    }
}

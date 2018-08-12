/*
 * Copyright (c) 2018, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: HttpBootstrap.java
 * Date: 2018-01-28
 * Author: sandao
 */

package org.smartboot.http.server;

import org.apache.commons.lang.math.NumberUtils;
import org.smartboot.http.common.HttpEntityV2;
import org.smartboot.http.common.HttpRequestProtocol;
import org.smartboot.http.common.utils.HttpHeaderConstant;
import org.smartboot.http.server.handle.HttpHandle;
import org.smartboot.http.server.http11.HttpResponse;
import org.smartboot.socket.Filter;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.extension.ssl.ClientAuth;
import org.smartboot.socket.extension.timer.QuickMonitorTimer;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSSLQuickServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

public class HttpBootstrap {

    public static void main(String[] args) throws UnknownHostException {
        HttpV2MessageProcessor processor = new HttpV2MessageProcessor(System.getProperty("webapps.dir", "./"));
        processor.route("/", new HttpHandle() {
            byte[] body = "welcome to smart-socket http server!".getBytes();

            @Override
            public void doHandle(HttpEntityV2 request, HttpResponse response) throws IOException {

                response.setHeader(HttpHeaderConstant.Names.CONTENT_LENGTH, body.length + "");
                response.getOutputStream().write(body);
            }
        });
        processor.route("/upload", new HttpHandle() {
            @Override
            public void doHandle(HttpEntityV2 request, HttpResponse response) throws IOException {
                InputStream in = request.getInputStream();
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = in.read(buffer)) != -1) {
                    System.out.println(new String(buffer, 0, len));
                }
                response.getOutputStream().write("Success".getBytes());
                in.close();
            }
        });
        http(processor);
//        https(processor);
    }

    public static void http(MessageProcessor<HttpEntityV2> processor) {
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        int port = NumberUtils.toInt(System.getProperty("port"), 8888);
        AioQuickServer<HttpEntityV2> server = new AioQuickServer<HttpEntityV2>(port, new HttpRequestProtocol(), processor);
//        server.setDirectBuffer(true);
        server.setWriteQueueSize(0);
        server.setReadBufferSize(1024);
//        server.setThreadNum(8);
        server.setFilters(new Filter[]{new QuickMonitorTimer<HttpEntityV2>()});
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void https(HttpV2MessageProcessor processor) {
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        AioSSLQuickServer<? extends HttpEntityV2> server = new AioSSLQuickServer<HttpEntityV2>(8889, new HttpRequestProtocol(), processor);
        server
                .setClientAuth(ClientAuth.OPTIONAL)
                .setKeyStore(ClassLoader.getSystemClassLoader().getResource("server.jks").getFile(), "storepass")
                .setTrust(ClassLoader.getSystemClassLoader().getResource("trustedCerts.jks").getFile(), "storepass")
                .setKeyPassword("keypass")
        ;
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

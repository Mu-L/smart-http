/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpServerTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.http.client.HttpClient;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.common.enums.HttpMethodEnum;
import org.smartboot.http.common.enums.HttpProtocolEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.server.HttpBootstrap;
import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.http.server.HttpServerHandler;
import org.smartboot.http.test.BastTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/4
 */
public class HttpServer2Test extends BastTest {
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_METHOD = "method";
    public static final String KEY_URI = "uri";
    public static final String KEY_URL = "url";
    public static final String KEY_HEADERS = "headers";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer2Test.class);
    HttpClient httpClient;
    HttpBootstrap bootstrap;

    @Before
    public void init() {
        bootstrap = new HttpBootstrap();
        bootstrap.configuration().debug(true);
        bootstrap.start();
        httpClient = getHttpClient();
    }

    @Test
    public void test1() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                response.setContentType("test");
                response.write("Hello World".getBytes(StandardCharsets.UTF_8));
            }
        }).setPort(SERVER_PORT);
        org.smartboot.http.client.HttpResponse httpResponse = httpClient.get("/").done().get();
        Assert.assertEquals(httpResponse.getContentType(), "test");
        Assert.assertEquals(httpResponse.getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName()), HeaderValueEnum.CHUNKED.getName());
    }

    @Test
    public void test2() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                response.write("Hello World".getBytes(StandardCharsets.UTF_8));
            }
        }).setPort(SERVER_PORT);
        org.smartboot.http.client.HttpResponse httpResponse = httpClient.get("/").done().get();
        Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
        Assert.assertEquals(httpResponse.getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName()), HeaderValueEnum.CHUNKED.getName());
    }

    @Test
    public void testPut() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                response.write("Hello World".getBytes(StandardCharsets.UTF_8));
            }
        }).setPort(SERVER_PORT);
        org.smartboot.http.client.HttpResponse httpResponse = httpClient.rest("/").setMethod(HttpMethodEnum.PUT.getMethod()).done().get();
        Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
        Assert.assertEquals(httpResponse.getStatus(), HttpStatus.OK.value());
    }

    @Test
    public void testPost() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                request.getInputStream().close();
                response.write("Hello World".getBytes(StandardCharsets.UTF_8));
            }
        }).setPort(SERVER_PORT);
        for (int i = 0; i < 10; i++) {
            String body = "hello" + i;
            org.smartboot.http.client.HttpResponse httpResponse = httpClient.post("/").header().setContentLength(body.length()).done().body().write(body).done().done().get();
            Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
            Assert.assertEquals(httpResponse.getStatus(), HttpStatus.OK.value());
            Assert.assertEquals(httpResponse.body(), "Hello World");
        }

    }

    @Test
    public void testPost1() throws ExecutionException, InterruptedException {
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                byte[] buffer = new byte[(int) request.getContentLength()];
                request.getInputStream().read(buffer);
//                request.getInputStream().close();
                response.write(buffer);
            }
        }).setPort(SERVER_PORT);
        for (int i = 0; i < 10; i++) {
            String body = "hello" + i;
            org.smartboot.http.client.HttpResponse httpResponse = httpClient.post("/").header().keepalive(true).setContentLength(body.length()).done().body().write(body).done().done().get();
            Assert.assertEquals(httpResponse.getProtocol(), HttpProtocolEnum.HTTP_11.getProtocol());
            Assert.assertEquals(httpResponse.getStatus(), HttpStatus.OK.value());
            Assert.assertEquals(httpResponse.body(), body);
        }

    }

    @After
    public void destroy() {
        httpClient.close();
        bootstrap.shutdown();
    }
}

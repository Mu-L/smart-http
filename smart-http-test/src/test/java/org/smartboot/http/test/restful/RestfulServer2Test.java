/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpServerTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.test.restful;

import com.alibaba.fastjson.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.http.client.HttpClient;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.restful.RestfulBootstrap;
import org.smartboot.http.server.HttpBootstrap;
import org.smartboot.http.test.BastTest;
import org.smartboot.http.test.server.RequestUnit;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/4
 */
public class RestfulServer2Test extends BastTest {
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_METHOD = "method";
    public static final String KEY_URI = "uri";
    public static final String KEY_URL = "url";
    public static final String KEY_HEADERS = "headers";
    private static final Logger LOGGER = LoggerFactory.getLogger(RestfulServer2Test.class);
    private HttpBootstrap bootstrap;
    private RequestUnit requestUnit;


    @Before
    public void init() throws Exception {
        bootstrap = RestfulBootstrap.getInstance().scan("org.smartboot.http.test.restful").bootstrap();
        bootstrap.setPort(SERVER_PORT);
        bootstrap.configuration().addPlugin(new StreamMonitorPlugin<>((asynchronousSocketChannel, bytes) -> System.out.println(new String(bytes)), (asynchronousSocketChannel, bytes) -> System.out.println(new String(bytes))));
        bootstrap.start();
    }

    @Test
    public void testGet() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();

        Assert.assertEquals(httpClient.get("/").done().get().body(), "hello");
    }

    @Test
    public void testGet2() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        Assert.assertEquals(httpClient.get("/demo2").done().get().body(), "hello world");
    }

    @Test
    public void testGet3() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        Assert.assertEquals(httpClient.get("/demo2/param1?param=param1").done().get().body(), "hello param1");
    }

    @Test
    public void testPost() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        Map<String, String> params = new HashMap<>();
        params.put("param", "paramPost");
        Assert.assertEquals(httpClient.post("/demo2/param1").body().formUrlencoded(params).done().get().body(), "hello paramPost");
    }

    @Test
    public void testPost1() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        Map<String, String> params = new HashMap<>();
        params.put("param1", "paramPost1");
        params.put("param2", "paramPost2");
        Assert.assertEquals(httpClient.post("/demo2/param2").body().formUrlencoded(params).done().get().body(), "hello paramPost1 paramPost2");
    }

    @Test
    public void testPost2() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        Map<String, String> params = new HashMap<>();
        params.put("param1", "paramPost1");
        params.put("param2", "paramPost2");
        Assert.assertEquals(httpClient.post("/demo2/param3").body().formUrlencoded(params).done().get().body(), "hello paramPost1 paramPost2");
    }

    @Test
    public void testPostJson() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("param1", "paramPost1");
        jsonObject.put("param2", "paramPost2");
        Assert.assertEquals(httpClient.post("/demo2/param3").header().setContentType(HeaderValueEnum.APPLICATION_JSON.getName()).done().body().write(jsonObject.toJSONString().getBytes()).done().done().get().body(), "hello paramPost1 paramPost2");
    }

    @Test
    public void testPostJson1() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("param1", "paramPost1");
        jsonObject.put("param2", "paramPost2");
        Assert.assertEquals(httpClient.post("/demo2/param4").header().setContentType(HeaderValueEnum.APPLICATION_JSON.getName()).done().body().write(jsonObject.toJSONString().getBytes()).done().done().get().body(), "hello param is null");
    }

    @Test
    public void testPostJson2() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("param1", "paramPost1");
        jsonObject.put("param2", "paramPost2");
        Assert.assertEquals(httpClient.post("/demo2/param5").header().setContentType(HeaderValueEnum.APPLICATION_JSON.getName()).done().body().write(jsonObject.toJSONString().getBytes()).done().done().get().body(), "hello param is null");
    }

    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}

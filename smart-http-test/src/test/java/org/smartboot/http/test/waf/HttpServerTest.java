/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpServerTest.java
 * Date: 2021-06-04
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.test.waf;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.http.client.HttpClient;
import org.smartboot.http.client.HttpGet;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.common.enums.HttpMethodEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.server.HttpBootstrap;
import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.http.server.HttpServerHandler;
import org.smartboot.http.test.BastTest;
import org.smartboot.http.test.server.RequestUnit;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPOutputStream;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/4
 */
public class HttpServerTest extends BastTest {
    public static final String KEY_PARAMETERS = "parameters";
    public static final String KEY_METHOD = "method";
    public static final String KEY_URI = "uri";
    public static final String KEY_URL = "url";
    public static final String KEY_HEADERS = "headers";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerTest.class);
    private HttpBootstrap bootstrap;
    private RequestUnit requestUnit;

    @Before
    public void init() {
        bootstrap = new HttpBootstrap();
        bootstrap.httpHandler(new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                //随机启用GZIP
                OutputStream outputStream;
                if (System.currentTimeMillis() % 2 == 0) {
                    response.setHeader(HeaderNameEnum.CONTENT_ENCODING.getName(), HeaderValueEnum.GZIP.getName());
                    outputStream = new GZIPOutputStream(response.getOutputStream());
                } else {
                    outputStream = response.getOutputStream();
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(KEY_METHOD, request.getMethod());
                jsonObject.put(KEY_URI, request.getRequestURI());
                jsonObject.put(KEY_URL, request.getRequestURL());

                Map<String, String> parameterMap = new HashMap<>();
                request.getParameters().keySet().forEach(parameter -> parameterMap.put(parameter, request.getParameter(parameter)));
                jsonObject.put(KEY_PARAMETERS, parameterMap);

                Map<String, String> headerMap = new HashMap<>();
                request.getHeaderNames().forEach(headerName -> headerMap.put(headerName, request.getHeader(headerName)));
                jsonObject.put(KEY_HEADERS, headerMap);

                outputStream.write(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
                outputStream.close();
            }
        }).setPort(SERVER_PORT);
        bootstrap.configuration().addPlugin(new StreamMonitorPlugin<>(StreamMonitorPlugin.BLUE_TEXT_INPUT_STREAM, StreamMonitorPlugin.RED_TEXT_OUTPUT_STREAM));
        bootstrap.start();

        requestUnit = new RequestUnit();
        requestUnit.setUri("/hello");
        Map<String, String> headers = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            headers.put("header_" + i, UUID.randomUUID().toString());
        }
        headers.put("header_empty", "");
        requestUnit.setHeaders(headers);
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            params.put("params_" + i, UUID.randomUUID().toString());
        }
        requestUnit.setParameters(params);
    }

    @Test
    public void testGet() throws ExecutionException, InterruptedException {
        bootstrap.configuration().getWafConfiguration()
                .getAllowMethods().add(HttpMethodEnum.POST.getMethod());
        HttpClient httpClient = getHttpClient();
        StringBuilder uriStr = new StringBuilder(requestUnit.getUri()).append("?");
        requestUnit.getParameters().forEach((key, value) -> uriStr.append(key).append('=').append(value).append('&'));
        HttpGet httpGet = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet.header().add(name, value));

        Assert.assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), httpGet.done().get().getStatus());
    }

    @Test
    public void testGet1() throws ExecutionException, InterruptedException {
        bootstrap.configuration().getWafConfiguration()
                .getAllowMethods().add(HttpMethodEnum.GET.getMethod());
        HttpClient httpClient = getHttpClient();
        StringBuilder uriStr = new StringBuilder(requestUnit.getUri()).append("?");
        requestUnit.getParameters().forEach((key, value) -> uriStr.append(key).append('=').append(value).append('&'));
        HttpGet httpGet = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet.header().add(name, value));
        JSONObject jsonObject = basicCheck(httpGet.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    @Test
    public void testURI() throws ExecutionException, InterruptedException {
        HttpClient httpClient = getHttpClient();
        StringBuilder uriStr = new StringBuilder(requestUnit.getUri()).append("?");
        requestUnit.getParameters().forEach((key, value) -> uriStr.append(key).append('=').append(value).append('&'));
        HttpGet httpGet = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet.header().add(name, value));
        JSONObject jsonObject = basicCheck(httpGet.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));

        bootstrap.configuration().getWafConfiguration()
                .getAllowUriPrefixes().add("/aa");
        HttpGet httpGet1 = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet1.header().add(name, value));
        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), httpGet1.done().get().getStatus());

        bootstrap.configuration().getWafConfiguration()
                .getAllowUriPrefixes().add("/hello");
        HttpGet httpGet2 = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet2.header().add(name, value));
        jsonObject = basicCheck(httpGet2.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));

        bootstrap.configuration().getWafConfiguration()
                .getAllowUriPrefixes().clear();
        bootstrap.configuration().getWafConfiguration().getAllowUriSuffixes().add("/aa");
        HttpGet httpGet3 = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet3.header().add(name, value));
        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), httpGet3.done().get().getStatus());

        bootstrap.configuration().getWafConfiguration()
                .getAllowUriSuffixes().add("llo");
        HttpGet httpGet4 = httpClient.get(uriStr.toString());
        requestUnit.getHeaders().forEach((name, value) -> httpGet4.header().add(name, value));
        jsonObject = basicCheck(httpGet4.done().get(), requestUnit);
        Assert.assertEquals(HttpMethodEnum.GET.getMethod(), jsonObject.get(KEY_METHOD));
    }

    private JSONObject basicCheck(org.smartboot.http.client.HttpResponse response, RequestUnit requestUnit) {
        JSONObject jsonObject = JSON.parseObject(response.body());
        LOGGER.info(JSON.toJSONString(jsonObject, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat));
        Assert.assertEquals(requestUnit.getUri(), jsonObject.get(KEY_URI));

        JSONObject headerJson = jsonObject.getJSONObject(KEY_HEADERS);
        requestUnit.getHeaders().forEach((key, value) -> {
            Assert.assertEquals(value, headerJson.get(key));
        });

        JSONObject parameters = jsonObject.getJSONObject(KEY_PARAMETERS);
        requestUnit.getParameters().forEach((key, value) -> {
            Assert.assertEquals(value, parameters.get(key));
        });
        return jsonObject;
    }


    @After
    public void destroy() {
        bootstrap.shutdown();
    }
}

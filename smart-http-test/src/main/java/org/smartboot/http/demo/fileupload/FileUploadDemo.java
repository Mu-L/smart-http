/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: FileUploadDemo.java
 * Date: 2020-04-03
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.demo.fileupload;

import org.smartboot.http.common.multipart.Part;
import org.smartboot.http.server.HttpBootstrap;
import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.http.server.HttpServerHandler;
import org.smartboot.http.server.handler.HttpRouteHandler;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author 三刀
 * @version V1.0 , 2019/11/24
 */
public class FileUploadDemo {
    public static void main(String[] args) {

        HttpRouteHandler routeHandler = new HttpRouteHandler();
        routeHandler.route("/", new HttpServerHandler() {
                    byte[] body = ("<html>" +
                            "<head><title>smart-http demo</title></head>" +
                            "<body>" +
                            "GET 表单提交<form action='/get' method='get'><input type='text' name='text'/><input type='submit'/></form></br>" +
                            "POST 表单提交<form action='/post' method='post'><input type='text' name='text'/><input type='submit'/></form></br>" +
                            "文件上传<form action='/upload' method='post' enctype='multipart/form-data'><input type='file' name='text'/><input type='submit'/></form></br>" +
                            "</body></html>").getBytes();

                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {

                        response.setContentLength(body.length);
                        response.getOutputStream().write(body);
                    }
                })
                .route("/upload", new HttpServerHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) throws IOException {
                        try {
                            for (Part part : request.getParts()) {
                                String name = part.getName();
                                InputStream stream = part.getInputStream();
                                if (part.isFormField()) {
                                    System.out.println("Form field " + name + " with value "
                                            + stream + " detected.");
                                } else {
                                    System.out.println("File field " + name + " with file name "
                                            + part.getName() + " detected.");
                                    // Process the input stream
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });


        HttpBootstrap bootstrap = new HttpBootstrap();
        //配置HTTP消息处理管道
        bootstrap.httpHandler(routeHandler);

        //设定服务器配置并启动
        bootstrap.start();
    }
}

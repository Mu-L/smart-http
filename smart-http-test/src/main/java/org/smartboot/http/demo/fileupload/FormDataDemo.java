package org.smartboot.http.demo.fileupload;

import org.smartboot.http.common.multipart.Part;
import org.smartboot.http.server.HttpBootstrap;
import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.http.server.HttpServerHandler;
import org.smartboot.http.server.handler.HttpRouteHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @Description: TODO
 * @Author MiSinG
 * @Date 2024/5/29
 * @Version V1.0
 **/
public class FormDataDemo {
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
                    public void handle(HttpRequest request, HttpResponse response) {
                        try {
                            for (Part item : request.getParts()) {
                                String name = item.getName();
                                System.out.println("name = " + name);
                                InputStream inputStream = item.getInputStream();
                                if (item.getSubmittedFileName() != null) {
                                    System.out.println("filename = " + item.getSubmittedFileName());
                                    //保存到指定路径
                                    Path filePath = Paths.get("smart-http-test", "src", "main", "resources").resolve(item.getSubmittedFileName());
                                    Files.createDirectories(filePath.getParent());
                                    Files.copy(inputStream, filePath);
                                    item.delete();
                                } else {
                                    //打印inputStream
                                    try (Scanner scanner = new Scanner(inputStream)) {
                                        while (scanner.hasNextLine()) {
                                            System.out.println(scanner.nextLine());
                                        }
                                    }
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

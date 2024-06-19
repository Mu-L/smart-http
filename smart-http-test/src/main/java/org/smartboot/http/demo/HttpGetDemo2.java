/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpGetDemo.java
 * Date: 2021-06-08
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.demo;

import org.smartboot.http.client.HttpClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpGetDemo2 {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        HttpClient httpClient = new HttpClient("www.baidu.com", 80);
        for (int i = 0; i < 100; i++) {
            int j=i;
            executorService.submit(() -> {
                httpClient.get("/").header().keepalive(false).done()
                        .onSuccess(response -> System.out.println(j))
                        .onFailure(Throwable::printStackTrace)
                        .done();
            });
        }
        // 关闭线程池并等待任务完成
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }


    }
}
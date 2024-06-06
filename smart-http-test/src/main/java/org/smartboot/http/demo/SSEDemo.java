package org.smartboot.http.demo;

import org.smartboot.http.restful.RestfulBootstrap;
import org.smartboot.http.restful.sse.SSEHandler;
import org.smartboot.http.restful.sse.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SSEDemo {
    public static void main(String[] args) throws Exception {
        RestfulBootstrap bootstrap = RestfulBootstrap.getInstance();
        bootstrap.bootstrap().httpHandler(new SSEHandler() {
            @Override
            public void onOpen(SseEmitter sseEmitter) {
                SSEHandler handler = this;
//                System.out.println("receive...:" + uid);
                Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        try {
                            sseEmitter.send(SseEmitter.event().name("update").comment("aaa").id(String.valueOf(i++)).data("hello world"));
                            if (i == 10) {
                                sseEmitter.complete();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, 1, 1, TimeUnit.SECONDS);
            }
        });
        bootstrap.bootstrap().configuration().debug(true);
        bootstrap.bootstrap().setPort(8080).start();
    }
}

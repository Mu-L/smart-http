package org.smartboot.http.restful.sse;

import org.smartboot.http.server.HttpRequest;
import org.smartboot.http.server.HttpResponse;
import org.smartboot.http.server.HttpServerHandler;
import org.smartboot.http.server.impl.Request;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public abstract class SSEHandler extends HttpServerHandler {
    private static final AttachKey<SseEmitter> SSE_EMITTER = AttachKey.valueOf("SSE_EMITTER");

    @Override
    public final void handle(HttpRequest request, HttpResponse response, CompletableFuture<Object> completableFuture) throws IOException {
        response.setHeader("Content-Type", "text/event-stream");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.getOutputStream().flush();
    }

    public abstract void onOpen(SseEmitter sseEmitter);


    @Override
    public final void onHeaderComplete(Request request) throws IOException {
        Attachment attachment = request.getAttachment();
        if (attachment == null) {
            attachment = new Attachment();
            request.setAttachment(attachment);
        }
        SseEmitter sseEmitter = new SseEmitter(request.getAioSession());
        attachment.put(SSE_EMITTER, sseEmitter);
        onOpen(sseEmitter);
    }

    @Override
    public final void onClose(Request request) {
        Attachment attachment = request.getAttachment();
        SseEmitter sseEmitter = attachment.get(SSE_EMITTER);
        sseEmitter.complete();
    }
}

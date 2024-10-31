/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpServerHandle.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server;

import org.smartboot.http.common.HeaderValue;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.enums.HttpTypeEnum;
import org.smartboot.http.server.h2.codec.DataFrame;
import org.smartboot.http.server.h2.codec.GoAwayFrame;
import org.smartboot.http.server.h2.codec.HeadersFrame;
import org.smartboot.http.server.h2.codec.Http2Frame;
import org.smartboot.http.server.h2.codec.SettingsFrame;
import org.smartboot.http.server.h2.codec.WindowUpdateFrame;
import org.smartboot.http.server.h2.hpack.DecodingCallback;
import org.smartboot.http.server.impl.AbstractResponse;
import org.smartboot.http.server.impl.Http2RequestImpl;
import org.smartboot.http.server.impl.Http2Session;
import org.smartboot.http.server.impl.HttpMessageProcessor;
import org.smartboot.http.server.impl.HttpRequestImpl;
import org.smartboot.http.server.impl.Request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Http消息处理器
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public abstract class Http2ServerHandler implements ServerHandler<HttpRequest, HttpResponse> {
    private static final byte[] H2C_PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes();
    private static final int FRAME_HEADER_SIZE = 9;
    private ServerHandler<HttpRequest, HttpResponse> serverHandler;
    private ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public final void onHeaderComplete(Request request) throws IOException {
        if (request.getRequestType() == HttpTypeEnum.HTTP_2) {
            if (!"PRI".equals(request.getMethod()) || !"*".equals(request.getUri()) || request.getHeaderSize() > 0) {
                throw new IllegalStateException();
            }
            Http2Session session = request.newHttp2Session();
            session.setState(Http2Session.STATE_PREFACE_SM);
        } else {
            //解析 Header 中的 setting
            String http2Settings = request.getHeader(HeaderNameEnum.HTTP2_SETTINGS.getName());
            byte[] bytes = Base64.getUrlDecoder().decode(http2Settings);
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            SettingsFrame settingsFrame = new SettingsFrame(0, 0, bytes.length);
            settingsFrame.decode(byteBuffer);
            Http2Session session = request.newHttp2Session();
            session.setState(Http2Session.STATE_PREFACE);
            //更新服务端的 setting
            session.updateSettings(settingsFrame);

            HttpRequestImpl req = request.newHttpRequest();
            AbstractResponse response = req.getResponse();
            response.setHttpStatus(HttpStatus.SWITCHING_PROTOCOLS);
            response.setContentType(null);
            response.setHeader(HeaderNameEnum.UPGRADE.getName(), HeaderValueEnum.H2C.getName());
            response.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValueEnum.UPGRADE.getName());
            OutputStream outputStream = response.getOutputStream();
            outputStream.flush();

        }
    }

    @Override
    public final void onBodyStream(ByteBuffer buffer, Request request) {
        Http2Session session = request.newHttp2Session();
        switch (session.getState()) {
            case Http2Session.STATE_FIRST_REQUEST: {
                HttpRequestImpl httpRequest = request.newHttpRequest();
                request.setType(HttpTypeEnum.HTTP_2);
//                httpServerHandler.onBodyStream(buffer, request);
                return;
            }
            case Http2Session.STATE_PREFACE_SM: {
                if (buffer.remaining() < 6) {
                    return;
                }
                for (int i = H2C_PREFACE.length - 6; i < H2C_PREFACE.length; i++) {
                    if (H2C_PREFACE[i] != buffer.get()) {
                        throw new IllegalStateException();
                    }
                }
                session.setPrefaced(true);
                session.setState(Http2Session.STATE_FRAME_HEAD);
                onBodyStream(buffer, request);
                return;
            }
            case Http2Session.STATE_PREFACE: {
                if (buffer.remaining() < H2C_PREFACE.length) {
                    break;
                }
                for (byte b : H2C_PREFACE) {
                    if (b != buffer.get()) {
                        throw new IllegalStateException();
                    }
                }
                session.setPrefaced(true);
                session.setState(Http2Session.STATE_FRAME_HEAD);
            }
            case Http2Session.STATE_FRAME_HEAD: {
                if (buffer.remaining() < FRAME_HEADER_SIZE) {
                    break;
                }
                Http2Frame frame = parseFrame(buffer);
                session.setCurrentFrame(frame);
                session.setState(Http2Session.STATE_FRAME_PAYLOAD);
            }
            case Http2Session.STATE_FRAME_PAYLOAD: {
                Http2Frame frame = session.getCurrentFrame();
                if (!frame.decode(buffer)) {
                    break;
                }
                session.setState(Http2Session.STATE_FRAME_HEAD);
                session.setCurrentFrame(null);
                try {
                    doHandler(frame, request);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                onBodyStream(buffer, request);
            }
        }
    }

    private void doHandler(Http2Frame frame, Request req) throws IOException {
        Http2Session session = req.newHttp2Session();
        switch (frame.type()) {
            case Http2Frame.FRAME_TYPE_SETTINGS: {
                SettingsFrame settingsFrame = (SettingsFrame) frame;
                if (settingsFrame.getFlag(SettingsFrame.ACK)) {
                    SettingsFrame settingAckFrame = new SettingsFrame(settingsFrame.streamId(), SettingsFrame.ACK, 0);
                    settingAckFrame.writeTo(req.getAioSession().writeBuffer());
                    req.getAioSession().writeBuffer().flush();
                    System.err.println("Setting ACK报文已发送");
//                    req.newHttp2Session().setState(Http2Session.STATE_FRAME_HEAD);
                } else {
                    System.out.println("settingsFrame:" + settingsFrame);
                    settingsFrame.writeTo(req.getAioSession().writeBuffer());
                    req.getAioSession().writeBuffer().flush();
                    System.err.println("Setting报文已发送");
                }
            }
            break;
            case Http2Frame.FRAME_TYPE_WINDOW_UPDATE: {
                WindowUpdateFrame windowUpdateFrame = (WindowUpdateFrame) frame;
                System.out.println(windowUpdateFrame.getUpdate());
                SettingsFrame ackFrame = new SettingsFrame(windowUpdateFrame.streamId(), SettingsFrame.ACK, 0);
                ackFrame.writeTo(req.getAioSession().writeBuffer());
            }
            break;
            case Http2Frame.FRAME_TYPE_HEADERS: {
                HeadersFrame headersFrame = (HeadersFrame) frame;
                System.out.println("headerFrame Stream:" + headersFrame.streamId());
                Http2RequestImpl request = session.getStream(headersFrame.streamId());
                request.checkState(Http2RequestImpl.STATE_HEADER_FRAME);
                Map<String, HeaderValue> headers = request.getHeaders();
                session.getHpackDecoder().decode(headersFrame.getFragment(), headersFrame.getFlag(Http2Frame.FLAG_END_HEADERS), new DecodingCallback() {
                    @Override
                    public void onDecoded(CharSequence n, CharSequence v) {
                        String name = n.toString();
                        String value = v.toString();
                        if (name.charAt(0) == ':') {
                            switch (name) {
                                case ":method":
                                    request.setMethod(value);
                                    break;
                                case ":path":
                                    request.setRequestURI(value);
                                    break;
                                case ":scheme":
                                case ":authority":
                                    return;
                            }
                        } else {
                            headers.put(name, new HeaderValue(name, value));
                        }
                    }
                });
                if (headersFrame.getFlag(Http2Frame.FLAG_END_HEADERS)) {
                    request.setState(Http2RequestImpl.STATE_DATA_FRAME);
                    onHeaderComplete(request);
                    if (request.getContentLength() > 0) {
                        request.setBody(new ByteArrayOutputStream((int) request.getContentLength()));
                    } else {
                        request.setBody(new ByteArrayOutputStream());
                    }
                }
                break;
            }
            case Http2Frame.FRAME_TYPE_DATA: {
                DataFrame dataFrame = (DataFrame) frame;
                Http2RequestImpl request = session.getStream(dataFrame.streamId());
                request.checkState(Http2RequestImpl.STATE_DATA_FRAME);
                request.getBody().write(dataFrame.getData());
                if (dataFrame.getFlag(DataFrame.FLAG_END_STREAM)) {
                    request.bodyDone();
                    handleHttpRequest(request);
                }
            }
            break;
            case Http2Frame.FRAME_TYPE_GOAWAY: {
                System.out.println("GoAwayFrame:" + ((GoAwayFrame) frame).getLastStream());
                break;
            }
            default:
                throw new IllegalStateException();
        }
    }


    private static Http2Frame parseFrame(ByteBuffer buffer) {
        int first = buffer.getInt();
        int length = first >> 8;
        int type = first & 0x0f;
        int flags = buffer.get();
        int streamId = buffer.getInt();
        if ((streamId & 0x80000000) != 0) {
            throw new IllegalStateException();
        }
        switch (type) {
            case Http2Frame.FRAME_TYPE_HEADERS:
                return new HeadersFrame(streamId, flags, length);
            case Http2Frame.FRAME_TYPE_SETTINGS:
                return new SettingsFrame(streamId, flags, length);
            case Http2Frame.FRAME_TYPE_WINDOW_UPDATE:
                return new WindowUpdateFrame(streamId, flags, length);
            case Http2Frame.FRAME_TYPE_DATA:
                return new DataFrame(streamId, flags, length);
            case Http2Frame.FRAME_TYPE_GOAWAY:
                return new GoAwayFrame(streamId, flags, length);
        }
        throw new IllegalStateException("invalid type :" + type);
    }

    protected void onHeaderComplete(Http2RequestImpl request) throws IOException {

    }

    public final void handleHttpRequest(Http2RequestImpl abstractRequest) {
        AbstractResponse response = abstractRequest.getResponse();
        CompletableFuture<Object> future = new CompletableFuture<>();
        try {
            handle(abstractRequest, response, future);
            abstractRequest.getResponse().close();
        } catch (Throwable e) {
            HttpMessageProcessor.responseError(response, e);
        }
    }

//    private void finishHttpHandle(Http2RequestImpl abstractRequest, CompletableFuture<Object> future) throws IOException {
//        if (future.isDone()) {
//           abstractRequest.getResponse().close();
//            return;
//        }
//
//        AioSession session = abstractRequest.request.getAioSession();
//        ReadListener readListener = abstractRequest.getInputStream().getReadListener();
//        if (readListener == null) {
//            session.awaitRead();
//        } else {
//            //todo
////            abstractRequest.request.setDecoder(session.readBuffer().hasRemaining() ? HttpRequestProtocol.BODY_READY_DECODER : HttpRequestProtocol.BODY_CONTINUE_DECODER);
//        }
//
//        Thread thread = Thread.currentThread();
//        AbstractResponse response = abstractRequest.getResponse();
//        future.thenRun(() -> {
//            try {
//                if (keepConnection(abstractRequest)) {
//                    finishResponse(abstractRequest);
//                    if (thread != Thread.currentThread()) {
//                        session.writeBuffer().flush();
//                    }
//                }
//            } catch (Exception e) {
//                HttpMessageProcessor.responseError(response, e);
//            } finally {
//                if (readListener == null) {
//                    session.signalRead();
//                }
//            }
//        }).exceptionally(throwable -> {
//            try {
//                HttpMessageProcessor.responseError(response, throwable);
//            } finally {
//                if (readListener == null) {
//                    session.signalRead();
//                }
//            }
//            return null;
//        });
//    }
//
//    private void finishResponse(HttpRequestImpl abstractRequest) throws IOException {
//        AbstractResponse response = abstractRequest.getResponse();
//        //关闭本次请求的输出流
//        BufferOutputStream bufferOutputStream = response.getOutputStream();
//        if (!bufferOutputStream.isClosed()) {
//            bufferOutputStream.close();
//        }
//        abstractRequest.reset();
//    }
}

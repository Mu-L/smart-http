package org.smartboot.http.client;

import org.smartboot.http.client.impl.WebSocketRequestImpl;
import org.smartboot.http.client.impl.WebSocketResponseImpl;
import org.smartboot.http.common.codec.websocket.CloseReason;
import org.smartboot.http.common.codec.websocket.Decoder;
import org.smartboot.http.common.codec.websocket.WebSocket;
import org.smartboot.http.common.enums.BodyStreamStatus;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.common.enums.HttpMethodEnum;
import org.smartboot.http.common.enums.HttpProtocolEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.logging.Logger;
import org.smartboot.http.common.logging.LoggerFactory;
import org.smartboot.http.common.utils.Constant;
import org.smartboot.http.common.utils.NumberUtils;
import org.smartboot.http.common.utils.WebSocketUtil;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.ssl.factory.ClientSSLContextFactory;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.util.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WebSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);

    private final WebSocketConfiguration configuration;

    /**
     * Header: Host
     */
    private final String hostHeader;
    /**
     * 客户端Client
     */
    private AioQuickClient client;

    /**
     * 绑定线程池资源组
     */
    private AsynchronousChannelGroup asynchronousChannelGroup;

    private boolean connected;

    private boolean firstConnected = true;

    /**
     * Http 解码协议
     */
    private final HttpResponseProtocol protocol = HttpResponseProtocol.INSTANCE;
    /**
     * 消息处理器
     */
    private final HttpMessageProcessor processor = new HttpMessageProcessor();

    private final String uri;
    private WebSocketRequestImpl request;


    public static void main(String[] args) throws IOException {
        WebSocketClient client = new WebSocketClient("ws://localhost:8080");
        client.configuration().debug(true);
        client.connect(new WebSocketListener() {
            @Override
            public void onOpen(WebSocketClient client, WebSocketResponse session) {
                try {
                    client.sendMessage("hello");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onMessage(WebSocketClient client, String message) {
                System.out.println(message);
            }
        });
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    client.sendMessage("aaa" + System.currentTimeMillis());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public WebSocketClient(String url) {
        int schemaIndex = url.indexOf("://");
        if (schemaIndex == -1) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        String schema = url.substring(0, schemaIndex);
        int uriIndex = url.indexOf("/", schemaIndex + 3);
        int portIndex = url.indexOf(":", schemaIndex + 3);
        boolean ws = Constant.SCHEMA_WS.equals(schema);
        boolean wss = !ws && Constant.SCHEMA_WSS.equals(schema);

        if (!ws && !wss) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        String host;
        int port;
        if (portIndex > 0) {
            host = url.substring(schemaIndex + 3, portIndex);
            port = NumberUtils.toInt(uriIndex > 0 ? url.substring(portIndex + 1, uriIndex) : url.substring(portIndex + 1), -1);
        } else if (uriIndex > 0) {
            host = url.substring(schemaIndex + 3, uriIndex);
            port = wss ? 443 : 80;
        } else {
            host = url.substring(schemaIndex + 3);
            port = wss ? 443 : 80;
        }
        if (port == -1) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        this.configuration = new WebSocketConfiguration(host, port);
        configuration.setWss(wss);
        hostHeader = configuration.getHost() + ":" + configuration.getPort();
        this.uri = uriIndex > 0 ? url.substring(uriIndex) : "/";

    }

    public WebSocketConfiguration configuration() {
        return configuration;
    }

    public void connect(WebSocketListener listener) throws IOException {
        if (connected) {
            AioSession session = client.getSession();
            if (session == null || session.isInvalid()) {
                close();
                connect(listener);
            }
            return;
        }

        try {
            if (firstConnected) {
                boolean noneSslPlugin = true;
                for (Plugin responsePlugin : configuration.getPlugins()) {
                    processor.addPlugin(responsePlugin);
                    if (responsePlugin instanceof SslPlugin) {
                        noneSslPlugin = false;
                    }
                }
                if (noneSslPlugin && configuration.isWss()) {
                    processor.addPlugin(new SslPlugin<>(new ClientSSLContextFactory()));
                }

                firstConnected = false;
            }
            connected = true;
            client = configuration.getProxy() == null ? new AioQuickClient(configuration.getHost(), configuration.getPort(), protocol, processor) : new AioQuickClient(configuration.getProxy().getProxyHost(), configuration.getProxy().getProxyPort(), protocol, processor);
            client.setBufferPagePool(configuration.getReadBufferPool(), configuration.getWriteBufferPool()).setReadBufferSize(configuration.readBufferSize());
            if (configuration.getConnectTimeout() > 0) {
                client.connectTimeout(configuration.getConnectTimeout());
            }
            if (asynchronousChannelGroup == null) {
                client.start();
            } else {
                client.start(asynchronousChannelGroup);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        AioSession session = client.getSession();
        CompletableFuture<WebSocketResponseImpl> completableFuture = new CompletableFuture<>();
        completableFuture.thenAccept(new Consumer<WebSocketResponseImpl>() {
            @Override
            public void accept(WebSocketResponseImpl webSocketResponse) {
                try {
                    switch (webSocketResponse.getFrameOpcode()) {
                        case WebSocketUtil.OPCODE_TEXT:
                            listener.onMessage(WebSocketClient.this, new String(webSocketResponse.getPayload(), StandardCharsets.UTF_8));
                            break;
                        case WebSocketUtil.OPCODE_BINARY:
                            listener.onMessage(WebSocketClient.this, webSocketResponse.getPayload());
                            break;
                        case WebSocketUtil.OPCODE_CLOSE:
                            try {
                                listener.onClose(WebSocketClient.this, webSocketResponse, new CloseReason(webSocketResponse.getPayload()));
                            } finally {
                                WebSocketClient.this.close();
                            }
                            break;
                        case WebSocketUtil.OPCODE_PING:
                            System.out.println("ping...");
                            WebSocketUtil.send(request.getOutputStream(), WebSocketUtil.OPCODE_PONG, webSocketResponse.getPayload(), 0, webSocketResponse.getPayload().length);
//                        webSocketResponse.pong(webSocketResponse.getPayload());
                            break;
                        case WebSocketUtil.OPCODE_PONG:
//                                handlePong(request, response);
                            break;
                        case WebSocketUtil.OPCODE_CONTINUE:
                            LOGGER.warn("unSupport OPCODE_CONTINUE now,ignore payload: {}", StringUtils.toHexString(webSocketResponse.getPayload()));
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                } catch (Throwable throwable) {
                    listener.onError(WebSocketClient.this, webSocketResponse, throwable);
//                throw throwable;
                } finally {
                    CompletableFuture<WebSocketResponseImpl> completableFuture = new CompletableFuture<>();
                    completableFuture.thenAccept(this);
                    webSocketResponse.setFuture(completableFuture);
                    webSocketResponse.reset();
                }
            }
        });
        WebSocketResponseImpl webSocketResponse = new WebSocketResponseImpl(session, completableFuture);
        webSocketResponse.setResponseHandler(new ResponseHandler() {
            @Override
            public void onHeaderComplete(AbstractResponse abstractResponse) throws IOException {
                WebSocketResponseImpl webSocketResponse = (WebSocketResponseImpl) abstractResponse;
                super.onHeaderComplete(webSocketResponse);
                if (webSocketResponse.getStatus() != HttpStatus.SWITCHING_PROTOCOLS.value()) {
                    listener.onClose(WebSocketClient.this, webSocketResponse, new CloseReason(CloseReason.WRONG_CODE, ""));
                    return;
                }
                listener.onOpen(WebSocketClient.this, webSocketResponse);
            }

            @Override
            public BodyStreamStatus onBodyStream(ByteBuffer buffer, AbstractResponse abstractResponse) {
                WebSocketResponseImpl webSocketResponse = (WebSocketResponseImpl) abstractResponse;
                Decoder decoder = webSocketResponse.getDecoder().decode(buffer, webSocketResponse);
                webSocketResponse.setDecoder(decoder);
                return decoder == WebSocket.PAYLOAD_FINISH ? BodyStreamStatus.Finish : BodyStreamStatus.Continue;
            }

        });
        ResponseAttachment attachment = session.getAttachment();
        attachment.setResponse(webSocketResponse);
        initRest();
    }

    private void initRest() throws IOException {
        request = new WebSocketRequestImpl(client.getSession());
        request.setUri(uri);
        request.setMethod(HttpMethodEnum.GET.getMethod());
        request.setProtocol(HttpProtocolEnum.HTTP_11.getProtocol());
        request.addHeader(HeaderNameEnum.HOST.getName(), hostHeader);
        request.addHeader(HeaderNameEnum.UPGRADE.getName(), HeaderValueEnum.WEBSOCKET.getName());
        request.setHeader(HeaderNameEnum.CONNECTION.getName(), HeaderValueEnum.UPGRADE.getName());
        request.setHeader(HeaderNameEnum.Sec_WebSocket_Key.getName(), generateSecWebSocketKey());
        request.setHeader(HeaderNameEnum.Sec_WebSocket_Version.getName(), "13");
//        request.setHeader(HeaderNameEnum.Sec_WebSocket_Protocol.getName(), HeaderValueEnum.PERMESSAGE_DEFLATE.getName());
        request.getOutputStream().flush();
    }

    /**
     * 在客户端握手中的|Sec-WebSocket-Key|头字段包括一个 base64 编码的值，如果解码，长度是 16 字节。
     */
    private String generateSecWebSocketKey() {
        byte[] keyBytes = new byte[16];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }


    public void sendMessage(String message) throws IOException {
        // 发送消息到服务器
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        WebSocketUtil.sendMask(request.getOutputStream(), WebSocketUtil.OPCODE_TEXT, bytes, 0, bytes.length);
        request.getOutputStream().flush();
    }

    public void setAsynchronousChannelGroup(AsynchronousChannelGroup asynchronousChannelGroup) {
        this.asynchronousChannelGroup = asynchronousChannelGroup;
    }

    public void close() {
        try {
            WebSocketUtil.sendMask(request.getOutputStream(), WebSocketUtil.OPCODE_CLOSE, new CloseReason(CloseReason.NORMAL_CLOSURE, "").toBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            connected = false;
            client.shutdownNow();
        }
    }

}

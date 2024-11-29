/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpServerConfiguration.java
 * Date: 2021-02-22
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server;

import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.utils.ByteTree;
import org.smartboot.http.common.utils.StringUtils;
import org.smartboot.http.server.impl.Request;
import org.smartboot.http.server.waf.WafConfiguration;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.SslPlugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/2/22
 */
public class HttpServerConfiguration {
    public static final String VERSION = "2.3";

    /**
     * 缓存
     */
    private final ByteTree<Object> byteCache = new ByteTree<>();
    /**
     * URI缓存
     */
    private final ByteTree<ServerHandler<?, ?>> uriByteTree = new ByteTree<>();

    private final ByteTree<HeaderNameEnum> headerNameByteTree = new ByteTree<>();

    /**
     * smart-socket 插件
     */
    private final List<Plugin<Request>> plugins = new ArrayList<>();

    /**
     * 是否启用控制台banner
     */
    private boolean bannerEnabled = true;
    /**
     * read缓冲区大小
     */
    private int readBufferSize = 1024;
    /**
     * write缓冲区大小
     */
    private int writeBufferSize = 1024;
    /**
     * 服务线程数
     */
    private int threadNum = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    private String host;
    /**
     * 解析的header数量上限
     */
    private int headerLimiter = 100;

    /**
     * 闲置超时时间，默认：1分钟
     */
    private long httpIdleTimeout = 60000;

    /**
     * 闲置超时时间，默认：1分钟
     */
    private long wsIdleTimeout = 120000;
    /**
     * 服务器名称
     */
    private String serverName = "smart-http";

    /**
     * 是否加密通信
     */
    private boolean secure;
    /**
     * 最大请求报文
     */
    private long maxRequestSize = Integer.MAX_VALUE;

    private boolean lowMemory = false;
    private BufferPagePool readBufferPool;
    private BufferPagePool writeBufferPool;
    private AsynchronousChannelGroup group;

    private HttpServerHandler httpServerHandler = new HttpServerHandler() {
        @Override
        public void handle(HttpRequest request, HttpResponse response) throws IOException {
            response.write("Hello smart-http".getBytes(StandardCharsets.UTF_8));
        }
    };
    private WebSocketHandler webSocketHandler;

    private Http2ServerHandler http2ServerHandler = new Http2ServerHandler() {
        @Override
        public void handle(HttpRequest request, HttpResponse response) throws IOException {
            response.write("Hello smart-http".getBytes(StandardCharsets.UTF_8));
        }
    };

    private final WafConfiguration wafConfiguration = new WafConfiguration();


    int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * 设置read缓冲区大小，读缓冲区的大小至少得能容纳 url 或者一个Header value的长度，否则将触发异常
     *
     * @param readBufferSize
     * @return
     */
    public HttpServerConfiguration readBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    int getThreadNum() {
        return threadNum;
    }

    public HttpServerConfiguration threadNum(int threadNum) {
        this.threadNum = threadNum;
        return this;
    }

    int getWriteBufferSize() {
        return writeBufferSize;
    }

    public HttpServerConfiguration writeBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
        return this;
    }

    String getHost() {
        return host;
    }

    public HttpServerConfiguration host(String host) {
        this.host = host;
        return this;
    }

    boolean isBannerEnabled() {
        return bannerEnabled;
    }

    public HttpServerConfiguration bannerEnabled(boolean bannerEnabled) {
        this.bannerEnabled = bannerEnabled;
        return this;
    }

    public int getHeaderLimiter() {
        return headerLimiter;
    }

    /**
     * 支持解析的Header上限,若客户端提交Header数超过该值，超过部分将被忽略
     *
     * @param headerLimiter
     */
    public HttpServerConfiguration headerLimiter(int headerLimiter) {
        this.headerLimiter = headerLimiter;
        return this;
    }

    /**
     * 启用 debug 模式后会打印码流
     */
    public HttpServerConfiguration debug(boolean debug) {
        plugins.removeIf(plugin -> plugin instanceof StreamMonitorPlugin);
        if (debug) {
            addPlugin(new StreamMonitorPlugin<>(StreamMonitorPlugin.BLUE_TEXT_INPUT_STREAM,
                    StreamMonitorPlugin.RED_TEXT_OUTPUT_STREAM));
        }
        return this;
    }

    public String serverName() {
        return serverName;
    }

    public HttpServerConfiguration serverName(String server) {
        if (server == null) {
            this.serverName = null;
        } else {
            this.serverName = StringUtils.trim(server).replaceAll("\r", "").replaceAll("\n", "");
        }
        return this;
    }

    public ByteTree<ServerHandler<?, ?>> getUriByteTree() {
        return uriByteTree;
    }

    public HttpServerHandler getHttpServerHandler() {
        return httpServerHandler;
    }

    public void setHttpServerHandler(HttpServerHandler httpServerHandler) {
        this.httpServerHandler = httpServerHandler;
    }

    public WebSocketHandler getWebSocketHandler() {
        return webSocketHandler;
    }

    public void setWebSocketHandler(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public Http2ServerHandler getHttp2ServerHandler() {
        return http2ServerHandler;
    }

    public void setHttp2ServerHandler(Http2ServerHandler http2ServerHandler) {
        this.http2ServerHandler = http2ServerHandler;
    }

    /**
     * 将字符串缓存至 ByteTree 中，在Http报文解析过程中将获得更好的性能表现。
     * 适用范围包括： URL、HeaderName、HeaderValue
     */
    public ByteTree<Object> getByteCache() {
        return byteCache;
    }

    public ByteTree<HeaderNameEnum> getHeaderNameByteTree() {
        return headerNameByteTree;
    }

    public HttpServerConfiguration addPlugin(Plugin<Request> plugin) {
        plugins.add(plugin);
        if (plugin instanceof SslPlugin) {
            secure = true;
        }
        return this;
    }

    public boolean isSecure() {
        return secure;
    }

    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public HttpServerConfiguration addPlugin(List<Plugin<Request>> plugins) {
        this.plugins.addAll(plugins);
        return this;
    }

    public List<Plugin<Request>> getPlugins() {
        return plugins;
    }

    public AsynchronousChannelGroup group() {
        return group;
    }

    public HttpServerConfiguration group(AsynchronousChannelGroup group) {
        this.group = group;
        return this;
    }

    public WafConfiguration getWafConfiguration() {
        return wafConfiguration;
    }

    public long getHttpIdleTimeout() {
        return httpIdleTimeout;
    }

    public HttpServerConfiguration setHttpIdleTimeout(long httpIdleTimeout) {
        this.httpIdleTimeout = httpIdleTimeout;
        return this;
    }

    public long getWsIdleTimeout() {
        return wsIdleTimeout;
    }

    public HttpServerConfiguration setWsIdleTimeout(long wsIdleTimeout) {
        this.wsIdleTimeout = wsIdleTimeout;
        return this;
    }

    boolean isLowMemory() {
        return lowMemory;
    }

    public BufferPagePool getReadBufferPool() {
        return readBufferPool;
    }

    public HttpServerConfiguration setReadBufferPool(BufferPagePool readBufferPool) {
        this.readBufferPool = readBufferPool;
        return this;
    }

    public BufferPagePool getWriteBufferPool() {
        return writeBufferPool;
    }

    public HttpServerConfiguration setWriteBufferPool(BufferPagePool writeBufferPool) {
        this.writeBufferPool = writeBufferPool;
        return this;
    }

    public HttpServerConfiguration setLowMemory(boolean lowMemory) {
        this.lowMemory = lowMemory;
        return this;
    }
}

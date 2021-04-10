/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: WebSocketRequest.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server;

import org.smartboot.http.common.enums.WebsocketStatus;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * WebSocket消息请求接口
 *
 * @author 三刀
 * @version V1.0 , 2020/4/1
 */
public interface WebSocketRequest {
    public WebsocketStatus getWebsocketStatus();

    public int getFrameOpcode();

    public byte[] getPayload();

    String getRequestURI();

    String getQueryString();

    Map<String, String[]> getParameters();

    InetSocketAddress getRemoteAddress();

    /**
     * 获取套接字绑定的本地地址。
     *
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取附件对象
     *
     * @param <A> 附件对象类型
     * @return 附件
     */
    <A> A getAttachment();

    /**
     * 存放附件，支持任意类型
     *
     * @param <A>        附件对象类型
     * @param attachment 附件对象
     */
    <A> void setAttachment(A attachment);
}

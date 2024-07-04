/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: BodyStream.java
 * Date: 2021-07-17
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.client;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/7/17
 */
public interface Body<T> {
    /**
     * 往缓冲区中写入数据
     */
    Body<T> write(byte[] bytes, int offset, int len);

    /**
     * 往缓冲区中写入数据
     */
//    voidvoidvoid write(byte[] bytes, int offset, int len, Consumer<Body<T>> consumer);

    /**
     * 往缓冲区中写入数据
     */
    void transferFrom(ByteBuffer buffer, Consumer<Body<T>> consumer);

    /**
     * 往缓冲区中写入数据
     */
    default Body<T> write(byte[] bytes) {
        write(bytes, 0, bytes.length);
        return this;
    }

    default Body<T> write(String str) {
        return write(str.getBytes());
    }

    /**
     * 输出缓冲区的数据
     */
    Body<T> flush();

    /**
     * 结束body流操作
     */
    T done();
}

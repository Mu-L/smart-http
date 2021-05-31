/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HeaderNameEnum.java
 * Date: 2020-04-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.common.enums;

/**
 * @author 三刀
 * @version V1.0 , 2018/12/6
 */
public enum HeaderValueEnum {
    CHUNKED("chunked"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    UPGRADE("Upgrade"),
    WEBSOCKET("websocket"),
    KEEPALIVE("Keep-Alive"),
    DEFAULT_CONTENT_TYPE("text/html; charset=utf-8");

    private String name;

    private byte[] bytes;

    private byte[] bytesWithColon;


    HeaderValueEnum(String name) {
        this.name = name;
        this.bytes = name.getBytes();
        this.bytesWithColon = ("\r\n" + name + ":").getBytes();
    }


    public String getName() {
        return name;
    }

    public boolean equals(byte[] bytes, int length) {
        if (this.bytes.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (this.bytes[i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    public byte[] getBytesWithColon() {
        return bytesWithColon;
    }
}

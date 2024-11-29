/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpUtils.java
 * Date: 2020-11-22
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.common.utils;

import org.smartboot.http.common.Cookie;
import org.smartboot.http.common.HeaderValue;
import org.smartboot.http.common.codec.h2.hpack.Encoder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2020/11/22
 */
public class HttpUtils {
    private static final String DOMAIN = "$Domain";
    private static final String VERSION = "$Version";
    private static final String PATH = "$Path";

    /**
     * 解码URI中的参数
     *
     * @param paramStr http参数字符串： aa=111&bb=222
     * @param paramMap 参数存放Map
     */
    public static void decodeParamString(String paramStr, Map<String, String[]> paramMap) {
        if (StringUtils.isBlank(paramStr)) {
            return;
        }
        String[] uriParamStrArray = StringUtils.split(paramStr, "&");
        for (String param : uriParamStrArray) {
            int index = param.indexOf("=");
            if (index == -1) {
                continue;
            }
            try {
                String key = StringUtils.substring(param, 0, index);
                String value = URLDecoder.decode(StringUtils.substring(param, index + 1), "utf8");
                String[] values = paramMap.get(key);
                if (values == null) {
                    paramMap.put(key, new String[]{value});
                } else {
                    String[] newValue = new String[values.length + 1];
                    System.arraycopy(values, 0, newValue, 0, values.length);
                    newValue[values.length] = value;
                    paramMap.put(key, newValue);
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<Cookie> decodeCookies(String cookieStr) {
        List<Cookie> cookies = new ArrayList<>();
        decode(cookies, cookieStr, 0, new HashMap<String, String>());
        return cookies;
    }

    private static void decode(List<Cookie> cookies, String cookieStr, int offset, Map<String, String> cache) {
        while (offset < cookieStr.length() && cookieStr.charAt(offset) == ' ') {
            offset++;
        }
        if (offset >= cookieStr.length()) {
            return;
        }
        int index = cookieStr.indexOf('=', offset);
        if (index == -1) {
            return;
        }
        String name = cookieStr.substring(offset, index);
        int end = cookieStr.indexOf(';', index);
        int trimEnd = end;
        if (trimEnd == -1) {
            trimEnd = cookieStr.length();
        }
        while (cookieStr.charAt(trimEnd - 1) == ' ') {
            trimEnd--;
        }
        String value = cookieStr.substring(index + 1, trimEnd);

        if (name.charAt(0) == '$') {
            if (cookies.isEmpty()) {
                cache.put(name, value);
            } else {
                Cookie cookie = cookies.get(cookies.size() - 1);
                switch (name) {
                    case DOMAIN:
                        cookie.setDomain(value);
                        break;
                    case PATH:
                        cookie.setPath(value);
                        break;
                }
            }
        } else {
            Cookie cookie = new Cookie(name, value);
            if (!cache.isEmpty()) {
                cache.forEach((key, v) -> {
                    switch (key) {
                        case DOMAIN:
                            cookie.setDomain(v);
                            break;
                        case PATH:
                            cookie.setPath(v);
                            break;
                    }
                });
                cache.clear();
            }
            cookies.add(cookie);
        }
        if (end != -1) {
            decode(cookies, cookieStr, end + 1, cache);
        }
    }

    public static List<ByteBuffer> HPackEncoder(Encoder encoder, Map<String, HeaderValue> headers) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        List<ByteBuffer> buffers = new ArrayList<>();

        for (Map.Entry<String, HeaderValue> entry : headers.entrySet()) {
            if (entry.getKey().charAt(0) != ':') {
                continue;
            }
            HeaderValue headerValue = entry.getValue();
            while (headerValue != null) {
                System.out.println("encode: " + entry.getKey() + ":" + entry.getValue().getValue());
                encoder.header(entry.getKey().toLowerCase(), headerValue.getValue());
                while (!encoder.encode(buffer)) {
                    buffer.flip();
                    buffers.add(buffer);
                    buffer = ByteBuffer.allocate(1024);
                }
                headerValue = headerValue.getNextValue();
            }
        }

        for (Map.Entry<String, HeaderValue> entry : headers.entrySet()) {
            if (entry.getKey().charAt(0) == ':') {
                continue;
            }

            HeaderValue headerValue = entry.getValue();
            while (headerValue != null) {
                System.out.println("encode: " + entry.getKey() + ":" + headerValue.getValue());
                encoder.header(entry.getKey().toLowerCase(), headerValue.getValue());
                while (!encoder.encode(buffer)) {
                    buffer.flip();
                    buffers.add(buffer);
                    buffer = ByteBuffer.allocate(1024);
                }
                headerValue = headerValue.getNextValue();
            }
        }
        buffer.flip();
        if (buffer.hasRemaining()) {
            buffers.add(buffer);
        }
        return buffers;
    }
}

/*******************************************************************************
 * Copyright (c) 2017-2020, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: FixedLengthFrameDecoder.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.decode;

import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.exception.HttpException;
import org.smartboot.http.common.multipart.MultipartConfig;
import org.smartboot.http.common.multipart.PartImpl;
import org.smartboot.http.common.utils.ByteTree;
import org.smartboot.http.common.utils.Constant;
import org.smartboot.http.common.utils.StringUtils;
import org.smartboot.http.server.ServerHandler;
import org.smartboot.http.server.impl.HttpRequestImpl;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 * 指定长度的解码器
 *
 * @author 三刀
 * @version V1.0 , 2017/10/20
 */
public class MultipartFormDecoder {
    private final byte[] boundary;

    private int state;

    private static final int STATE_END_CHECK = 0;
    private static final int STATE_END = 1;
    private static final int STATE_PART_HEADER_NAME = 1 << 1;
    private static final int STATE_PART_HEADER_VALUE = 1 << 2;
    private static final int STATE_CONTENT_DISPOSITION_DECODER = 1 << 3;
    private static final int STATE_HEADER_END = 1 << 5;
    private static final int STATE_PART_VALUE_DECODE = 1 << 6;

    private static final int STATE_PART_FILE_DECODE = 1 << 7;

    private PartImpl currentPart;
    private final MultipartConfig multipartConfig;
    private long writeFileSize;

    public MultipartFormDecoder(HttpRequestImpl request, MultipartConfig configElement) {
        this.boundary = ("--" + request.getContentType().substring(request.getContentType().indexOf("boundary=") + 9)).getBytes();
        this.multipartConfig = configElement;
    }

    public boolean decode(ByteBuffer byteBuffer, HttpRequestImpl request) {
        switch (state) {
            case STATE_END_CHECK: {
                if (byteBuffer.remaining() < boundary.length + 2) {
                    return false;
                }
                for (byte b : boundary) {
                    if (byteBuffer.get() != b) {
                        throw new HttpException(HttpStatus.BAD_REQUEST);
                    }
                }
                byte b = byteBuffer.get();
                if (b == '-' && byteBuffer.get() == '-') {
                    state = STATE_END;
                    return decode(byteBuffer, request);
                } else if (byteBuffer.get() == Constant.LF) {
                    state = STATE_PART_HEADER_NAME;
                    currentPart = new PartImpl(multipartConfig);
                    request.setPart(currentPart);
                    return decode(byteBuffer, request);
                } else {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
            }
            case STATE_END: {
                if (byteBuffer.remaining() < 2) {
                    return false;
                }
                if (byteBuffer.get() == Constant.CR && byteBuffer.get() == Constant.LF) {
                    return true;
                }
                throw new HttpException(HttpStatus.BAD_REQUEST);
            }
            case STATE_PART_HEADER_NAME: {
                if (byteBuffer.remaining() < 2) {
                    return false;
                }
                //header解码结束
                byteBuffer.mark();
                if (byteBuffer.get() == Constant.CR) {
                    if (byteBuffer.get() != Constant.LF) {
                        throw new HttpException(HttpStatus.BAD_REQUEST);
                    }
                    //区分文件和普通字段
                    if (currentPart.getSubmittedFileName() == null) {
                        state = STATE_PART_VALUE_DECODE;
                    } else {
                        state = STATE_PART_FILE_DECODE;
                    }
                    return decode(byteBuffer, request);
                }
                byteBuffer.reset();
                //Header name解码
                ByteTree<HeaderNameEnum> name = StringUtils.scanByteTree(byteBuffer, ByteTree.COLON_END_MATCHER, request.getRequest().getConfiguration().getHeaderNameByteTree());
                if (name == null) {
                    return false;
                }
                System.out.println("headerName: " + name.getStringValue());
                currentPart.setHeaderTemp(name.getStringValue());
                if (HeaderNameEnum.CONTENT_DISPOSITION.getName().equals(name.getStringValue())) {
                    state = STATE_CONTENT_DISPOSITION_DECODER;
                } else {
                    state = STATE_PART_HEADER_VALUE;
                }
                return decode(byteBuffer, request);
            }
            case STATE_PART_HEADER_VALUE: {
                ByteTree<?> value = StringUtils.scanByteTree(byteBuffer, ByteTree.CR_END_MATCHER, request.getRequest().getConfiguration().getByteCache());
                if (value == null) {
                    if (byteBuffer.remaining() == byteBuffer.capacity()) {
                        throw new HttpException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
                    }
                    return false;
                }
                currentPart.setHeadValue(value.getStringValue());
                state = STATE_HEADER_END;
                return decode(byteBuffer, request);
            }
            case STATE_CONTENT_DISPOSITION_DECODER: {
                ByteTree<?> value = StringUtils.scanByteTree(byteBuffer, ByteTree.CR_END_MATCHER, request.getRequest().getConfiguration().getByteCache());
                if (value == null) {
                    if (byteBuffer.remaining() == byteBuffer.capacity()) {
                        throw new HttpException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
                    }
                    return false;
                }
                currentPart.setHeadValue(value.getStringValue());
                for (String partVal : value.getStringValue().split(";")) {
                    partVal = partVal.trim();
                    if (StringUtils.startsWith(partVal, "filename")) {
                        if (partVal.charAt(8) == '=') {
                            currentPart.setFileName(StringUtils.substring(partVal, 10, partVal.length() - 1));
                        } else if (partVal.charAt(8) == '*' && partVal.charAt(9) == '=') {
                            int characterSetIndex = partVal.indexOf('\'', 10);
                            int languageIndex = partVal.indexOf('\'', characterSetIndex + 1);
                            String characterSet = partVal.substring(10, characterSetIndex);
                            try {
                                String fileNameURLEncoded = partVal.substring(languageIndex + 1);
                                currentPart.setFileName(URLDecoder.decode(fileNameURLEncoded, characterSet));
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            throw new HttpException(HttpStatus.BAD_REQUEST);
                        }
                    } else if (StringUtils.startsWith(partVal, "name")) {
                        currentPart.setName(StringUtils.substring(partVal, partVal.indexOf("=\"") + 2, partVal.length() - 1));
                    }
                }
                state = STATE_HEADER_END;
            }
            case STATE_HEADER_END: {
                if (!byteBuffer.hasRemaining()) {
                    return false;
                }
                if (byteBuffer.get() != Constant.LF) {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                state = STATE_PART_HEADER_NAME;
                return decode(byteBuffer, request);
            }
            case STATE_PART_VALUE_DECODE: {
                // 判断是否是结束标记
                byteBuffer.mark();
                int boundaryLimit = findBoundary(byteBuffer);
                byteBuffer.reset();

                if (boundaryLimit < 0) {
                    if (byteBuffer.remaining() == byteBuffer.capacity()) {
                        throw new HttpException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
                    }
                    return false;
                }
                byte[] bytes = new byte[boundaryLimit];
                byteBuffer.get(bytes);
                currentPart.setInputStream(new ByteArrayInputStream(bytes));
                currentPart.setFormSize(bytes.length);
                currentPart = null;
                if (byteBuffer.get() != Constant.CR || byteBuffer.get() != Constant.LF) {
                    throw new HttpException(HttpStatus.BAD_REQUEST);
                }
                state = STATE_END_CHECK;
                return decode(byteBuffer, request);
            }
            case STATE_PART_FILE_DECODE: {
                if (byteBuffer.remaining() < boundary.length + 2) {
                    return false;
                }

                // 判断是否是结束标记
                byteBuffer.mark();
                int boundaryLimit = findBoundary(byteBuffer);
                byteBuffer.reset();

                byte[] bytes = boundaryLimit >= 0 ? new byte[boundaryLimit] : new byte[byteBuffer.remaining() - boundary.length - 2];
                byteBuffer.get(bytes);
                if (multipartConfig.getMaxFileSize() > 0) {
                    writeFileSize += bytes.length;
                    if (writeFileSize > multipartConfig.getMaxFileSize()) {
                        throw new HttpException(HttpStatus.PAYLOAD_TOO_LARGE);
                    }
                }
                try {
                    currentPart.getDiskOutputStream().write(bytes);
                    if (boundaryLimit >= 0) {
                        if (byteBuffer.get() != Constant.CR || byteBuffer.get() != Constant.LF) {
                            throw new HttpException(HttpStatus.BAD_REQUEST);
                        }
                        currentPart.getDiskOutputStream().flush();
                        currentPart.getDiskOutputStream().close();

                        currentPart = null;
                        state = STATE_END_CHECK;
                        return decode(byteBuffer, request);
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            default:
                throw new HttpException(HttpStatus.BAD_REQUEST);
        }
    }

    private int findBoundary(ByteBuffer byteBuffer) {
        int position = byteBuffer.position();
        while (byteBuffer.remaining() >= boundary.length + 2) {
            boolean match = true;
            for (int i = 0; i < boundary.length; i++) {
                if (boundary[i] != byteBuffer.get()) {
                    match = false;
                    if (i > 0) {
                        byteBuffer.position(byteBuffer.position() - i);
                    }
                    break;
                }
            }
            //完成匹配，跳出
            if (match) {
                return byteBuffer.position() - position - boundary.length - 2;
            }
        }
        return -1;
    }
}

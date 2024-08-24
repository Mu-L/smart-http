/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpServerHandle.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server;

import org.smartboot.http.common.ChunkedFrameDecoder;
import org.smartboot.http.common.Multipart;
import org.smartboot.http.common.enums.BodyStreamStatus;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.enums.HeaderValueEnum;
import org.smartboot.http.common.enums.HttpMethodEnum;
import org.smartboot.http.common.enums.HttpStatus;
import org.smartboot.http.common.exception.HttpException;
import org.smartboot.http.common.utils.FixedLengthFrameDecoder;
import org.smartboot.http.common.utils.SmartDecoder;
import org.smartboot.http.common.utils.StringUtils;
import org.smartboot.http.server.decode.Decoder;
import org.smartboot.http.server.decode.multipart.BoundaryDecoder;
import org.smartboot.http.server.impl.Request;
import org.smartboot.socket.util.AttachKey;
import org.smartboot.socket.util.Attachment;

import java.nio.ByteBuffer;

/**
 * Http消息处理器
 *
 * @author 三刀
 * @version V1.0 , 2018/2/6
 */
public abstract class HttpServerHandler implements ServerHandler<HttpRequest, HttpResponse> {

    @Override
    public BodyStreamStatus onBodyStream(ByteBuffer buffer, Request request) {
        if (HttpMethodEnum.GET.getMethod().equals(request.getMethod())) {
            return BodyStreamStatus.Finish;
        }
        int postLength = request.getContentLength();
        //Post请求
        if (HttpMethodEnum.POST.getMethod().equals(request.getMethod())
                && StringUtils.startsWith(request.getContentType(), HeaderValueEnum.X_WWW_FORM_URLENCODED.getName())
                && !HeaderValueEnum.UPGRADE.getName().equals(request.getHeader(HeaderNameEnum.CONNECTION.getName()))) {
            if (postLength == 0) {
                return BodyStreamStatus.Finish;
            }

            SmartDecoder smartDecoder = request.getBodyDecoder();
            if (smartDecoder == null) {
                if (postLength > 0) {
                    smartDecoder = new FixedLengthFrameDecoder(postLength);
                } else if (HeaderValueEnum.CHUNKED.getName().equals(request.getHeader(HeaderNameEnum.TRANSFER_ENCODING.getName()))) {
                    smartDecoder = new ChunkedFrameDecoder();
                } else {
                    throw new HttpException(HttpStatus.LENGTH_REQUIRED);
                }
                request.setBodyDecoder(smartDecoder);
            }

            if (smartDecoder.decode(buffer)) {
                request.setFormUrlencoded(smartDecoder.getBuffer());
                request.setBodyDecoder(null);
                return BodyStreamStatus.Finish;
            } else {
                return BodyStreamStatus.Continue;
            }
        } else if (HttpMethodEnum.POST.getMethod().equals(request.getMethod())
                && StringUtils.startsWith(request.getContentType(), HeaderValueEnum.MULTIPART_FORM_DATA.getName())
                && !HeaderValueEnum.UPGRADE.getName().equals(request.getHeader(HeaderNameEnum.CONNECTION.getName()))) {
            if (postLength < 0) {
                throw new HttpException(HttpStatus.LENGTH_REQUIRED);
            } else if (postLength == 0) {
                return BodyStreamStatus.Finish;
            }else if(buffer.position() == buffer.limit()){
                return BodyStreamStatus.Continue;
            }

            //form data 请求
            Decoder multipartDecoder = request.getMultipartDecoder();
            if (multipartDecoder == null) {
                multipartDecoder = BoundaryDecoder.getInstance(request.getConfiguration());
                String boundary = BoundaryDecoder.getBoundary(request.getContentType());
                Multipart multipart = new Multipart(boundary.getBytes());
                multipart.setBodyLength(postLength);
                request.setMultipart(multipart);
                request.setMultipartDecoder(multipartDecoder);
            }

            Decoder decode = multipartDecoder.decode(buffer, request);
            if (decode == null){
                return BodyStreamStatus.Finish;
            }else {
                request.setMultipartDecoder(decode);
                return BodyStreamStatus.Continue;
            }
        } else {
            return BodyStreamStatus.Finish;
        }
    }

}

/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpOutputStream.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.server.impl;

import org.smartboot.http.common.HeaderValue;
import org.smartboot.http.server.h2.codec.*;
import org.smartboot.http.server.h2.hpack.Encoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
final class Http2OutputStream extends AbstractOutputStream {
    private final int streamId;
    private boolean push;
    private final Http2Session http2Session;

    public Http2OutputStream(int streamId, Http2RequestImpl httpRequest, Http2ResponseImpl response, boolean push) {
        super(httpRequest, response);
        disableChunked();
        this.http2Session = httpRequest.getSession();
        this.streamId = streamId;
        this.push = push;
    }

    protected void writeHeader(HeaderWriteSource source) throws IOException {
        if (committed) {
            if (source == HeaderWriteSource.CLOSE && !closed) {
                DataFrame dataFrame1 = new DataFrame(streamId, DataFrame.FLAG_END_STREAM, 0);
                dataFrame1.writeTo(writeBuffer, new byte[0], 0, 0);
                writeBuffer.flush();
                System.out.println("close..., stream:" + streamId);
            }
            return;
        }
        // Create HEADERS frame



        response.setHeader(":status",String.valueOf(response.getHttpStatus()));
        List<ByteBuffer> buffers = new ArrayList<>();
        Encoder encoder = http2Session.getHpackEncoder();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        for (Map.Entry<String, HeaderValue> entry : response.getHeaders().entrySet()) {
            encoder.header(entry.getKey(), entry.getValue().getValue());
            while (!encoder.encode(buffer)) {
                buffer.flip();
                buffers.add(buffer);
                buffer = ByteBuffer.allocate(1024);
            }
        }
        buffer.flip();
        if(buffer.hasRemaining()){
            buffers.add(buffer);
        }

        boolean multipleHeaders = buffers.size()>1;
        if (push) {
            PushPromiseFrame headersFrame = new PushPromiseFrame(http2Session, streamId, multipleHeaders?0:Http2Frame.FLAG_END_HEADERS, 0);
            headersFrame.setFragment(buffers.isEmpty()?null:buffers.get(0));
            headersFrame.writeTo(writeBuffer);
        } else {
            HeadersFrame headersFrame = new HeadersFrame(http2Session, streamId, multipleHeaders?0:Http2Frame.FLAG_END_HEADERS, 0);
            headersFrame.setFragment(buffers.isEmpty()?null:buffers.get(0));
            headersFrame.writeTo(writeBuffer);
        }
        for(int i = 1; i < buffers.size()-1; i++){
            ContinuationFrame continuationFrame = new ContinuationFrame(null,  0, 0);
            continuationFrame.setFragment(buffers.get(i));
            continuationFrame.writeTo(writeBuffer);
        }
        if(multipleHeaders){
            ContinuationFrame continuationFrame = new ContinuationFrame(null,  Http2Frame.FLAG_END_HEADERS, 0);
            continuationFrame.setFragment(buffers.get(buffers.size()-1));
            continuationFrame.writeTo(writeBuffer);
        }
        writeBuffer.flush();
        System.err.println("StreamID: " + streamId + " Header已发送...");
        committed = true;
    }

    protected byte[] getHeadPart(boolean hasHeader) {
        //编码成http2
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeHeader(HeaderWriteSource.WRITE);
        System.out.println("write streamId:" + streamId);
        DataFrame dataFrame = new DataFrame(streamId, 0, len);
        dataFrame.writeTo(writeBuffer, b, off, len);
    }

}

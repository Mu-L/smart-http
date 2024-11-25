package org.smartboot.http.server.impl;

import org.smartboot.http.common.DecodeState;
import org.smartboot.http.common.enums.HeaderNameEnum;
import org.smartboot.http.common.utils.ByteTree;

public class DecoderUnit extends DecodeState {
    private ByteTree<HeaderNameEnum> decodeHeaderName;

    public DecoderUnit() {
        super(DecodeState.STATE_METHOD);
    }

    public ByteTree<HeaderNameEnum> getDecodeHeaderName() {
        return decodeHeaderName;
    }

    public void setDecodeHeaderName(ByteTree<HeaderNameEnum> decodeHeaderName) {
        this.decodeHeaderName = decodeHeaderName;
    }
}

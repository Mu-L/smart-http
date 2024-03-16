package org.smartboot.http.common.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Arrays;

public class WebSocketUtil {
    public static final byte OPCODE_CONTINUE = 0x0;
    public static final byte OPCODE_TEXT = 0x1;
    public static final byte OPCODE_BINARY = 0x2;
    public static final byte OPCODE_CLOSE = 0x8;
    public static final byte OPCODE_PING = 0x9;
    public static final byte OPCODE_PONG = 0xA;
    private static final byte[] maskKey = new byte[4];
    private static final SecureRandom secureRandom = new SecureRandom();

    static {
        secureRandom.nextBytes(maskKey);
    }

    public static void send(OutputStream outputStream, byte opCode, byte[] bytes) throws IOException {
        send(outputStream, opCode, bytes, 0, bytes.length);
    }

    public static void send(OutputStream outputStream, byte opCode, byte[] bytes, int offset, int len) throws IOException {
        int maxlength;
        if (len < Constant.WS_PLAY_LOAD_126) {
            maxlength = 2 + len;
        } else {
            maxlength = 4 + Math.min(Constant.WS_DEFAULT_MAX_FRAME_SIZE, len);
        }
        byte[] writBytes = new byte[maxlength];
        do {
            int payloadLength = len - offset;
            if (payloadLength > Constant.WS_DEFAULT_MAX_FRAME_SIZE) {
                payloadLength = Constant.WS_DEFAULT_MAX_FRAME_SIZE;
            }
            byte firstByte = offset + payloadLength < len ? (byte) 0x00 : (byte) 0x80;
            if (offset == 0) {
                firstByte |= opCode;
            } else {
                firstByte |= OPCODE_CONTINUE;
            }
            byte secondByte = payloadLength < Constant.WS_PLAY_LOAD_126 ? (byte) payloadLength : Constant.WS_PLAY_LOAD_126;
            writBytes[0] = firstByte;
            writBytes[1] = secondByte;
            if (secondByte == Constant.WS_PLAY_LOAD_126) {
                writBytes[2] = (byte) (payloadLength >> 8 & 0xff);
                writBytes[3] = (byte) (payloadLength & 0xff);
                System.arraycopy(bytes, offset, writBytes, 4, payloadLength);
            } else {
                System.arraycopy(bytes, offset, writBytes, 2, payloadLength);
            }
            outputStream.write(writBytes, 0, payloadLength < Constant.WS_PLAY_LOAD_126 ? 2 + payloadLength : 4 + payloadLength);
            offset += payloadLength;
        } while (offset < len);
    }

    public static void sendMask(OutputStream outputStream, byte opCode, byte[] bytes) throws IOException {
        sendMask(outputStream, opCode, bytes, 0, bytes.length);
    }

    public static void sendMask(OutputStream outputStream, byte opCode, byte[] bytes, int offset, int len) throws IOException {
//        // 生成掩码密钥值
//        byte[] maskKey = new byte[4];
//        Random random = new Random();
//        random.nextBytes(maskKey);

        int maxlength;
        if (len < Constant.WS_PLAY_LOAD_126) {
            maxlength = 6 + len;
        } else {
            maxlength = 8 + Math.min(Constant.WS_DEFAULT_MAX_FRAME_SIZE, len);
        }
        byte[] writBytes = new byte[maxlength];
        do {
            int payloadLength = len - offset;
            if (payloadLength > Constant.WS_DEFAULT_MAX_FRAME_SIZE) {
                payloadLength = Constant.WS_DEFAULT_MAX_FRAME_SIZE;
            }
            byte firstByte = offset + payloadLength < len ? (byte) 0x00 : (byte) 0x80;
            if (offset == 0) {
                firstByte |= opCode;
            } else {
                firstByte |= OPCODE_CONTINUE;
            }
            byte secondByte = payloadLength < Constant.WS_PLAY_LOAD_126 ? (byte) payloadLength : Constant.WS_PLAY_LOAD_126;
            writBytes[0] = firstByte;
            writBytes[1] = (byte) (secondByte | 0x80);
            if (secondByte == Constant.WS_PLAY_LOAD_126) {
                writBytes[2] = (byte) (payloadLength >> 8 & 0xff);
                writBytes[3] = (byte) (payloadLength & 0xff);
                System.arraycopy(maskKey, 0, writBytes, 4, maskKey.length);
                // 对消息进行掩码处理
                for (int i = 0; i < payloadLength; i++) {
                    writBytes[8 + i] = (byte) (bytes[i] ^ maskKey[i % 4]);
                }
            } else {
                System.out.println(Arrays.toString(maskKey));
                System.arraycopy(maskKey, 0, writBytes, 2, maskKey.length);
                // 对消息进行掩码处理
                for (int i = 0; i < payloadLength; i++) {
                    writBytes[6 + i] = (byte) (bytes[i] ^ maskKey[i % 4]);
                }
            }
            outputStream.write(writBytes, 0, payloadLength < Constant.WS_PLAY_LOAD_126 ? 6 + payloadLength : 8 + payloadLength);
            offset += payloadLength;
        } while (offset < len);
    }
}

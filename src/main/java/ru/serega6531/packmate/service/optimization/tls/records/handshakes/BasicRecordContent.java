package ru.serega6531.packmate.service.optimization.tls.records.handshakes;

import org.pcap4j.util.ByteArrays;

public class BasicRecordContent implements HandshakeRecordContent {

    /**
     * 0x0          - Content
     * 0x0 + length - End
     */

    private byte[] content;

    public static BasicRecordContent newInstance(byte[] rawData, int offset, int length) {
        if(length > 0) {
            ByteArrays.validateBounds(rawData, offset, length);
        }
        return new BasicRecordContent(rawData, offset, length);
    }

    public BasicRecordContent(byte[] rawData, int offset, int length) {
        content = new byte[length];
        if (length > 0) {
            System.arraycopy(rawData, offset, content, 0, length);
        }
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "    [" + content.length + " bytes]";
    }
}

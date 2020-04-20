package ru.serega6531.packmate.service.optimization.tls.records.handshakes;

import org.pcap4j.util.ByteArrays;

public class UnknownRecordContent implements HandshakeRecordContent {

    /**
     * 0x0          - Content
     * 0x0 + length - End
     */

    private byte[] content;

    public static UnknownRecordContent newInstance(byte[] rawData, int offset, int length) {
        ByteArrays.validateBounds(rawData, offset, length);
        return new UnknownRecordContent(rawData, offset, length);
    }

    public UnknownRecordContent(byte[] rawData, int offset, int length) {
        System.arraycopy(rawData, offset, content, 0, length);
    }

    @Override
    public String toString() {
        return "    [" + content.length + " bytes]";
    }
}

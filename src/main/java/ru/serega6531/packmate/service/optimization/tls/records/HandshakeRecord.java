package ru.serega6531.packmate.service.optimization.tls.records;

import org.pcap4j.util.ByteArrays;
import ru.serega6531.packmate.service.optimization.tls.numbers.HandshakeType;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.ClientHelloHandshakeRecordContent;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.HandshakeRecordContent;

import static org.pcap4j.util.ByteArrays.BYTE_SIZE_IN_BYTES;

public class HandshakeRecord extends TlsRecord {

    private static final int HANDSHAKE_TYPE_OFFSET = 0;

    private HandshakeType handshakeType;
    private HandshakeRecordContent content;

    public static HandshakeRecord newInstance(byte[] rawData, int offset, int length) {
        return new HandshakeRecord(rawData, offset, length);
    }

    private HandshakeRecord(byte[] rawData, int offset, int length) {
        this.handshakeType = HandshakeType.getInstance(ByteArrays.getByte(rawData, HANDSHAKE_TYPE_OFFSET + offset));

        if (handshakeType == HandshakeType.HELLO_REQUEST) {

        } else if (handshakeType == HandshakeType.CLIENT_HELLO) {
            this.content = ClientHelloHandshakeRecordContent.newInstance(
                    rawData, offset + BYTE_SIZE_IN_BYTES, length);
        } else if (handshakeType == HandshakeType.SERVER_HELLO) {

        } else if (handshakeType == HandshakeType.CERTIFICATE) {

        } else if (handshakeType == HandshakeType.SERVER_KEY_EXCHANGE) {

        } else if (handshakeType == HandshakeType.CERTIFICATE_REQUEST) {

        } else if (handshakeType == HandshakeType.SERVER_HELLO_DONE) {

        } else if (handshakeType == HandshakeType.CERTIFICATE_VERIFY) {

        } else if (handshakeType == HandshakeType.CLIENT_KEY_EXCHANGE) {

        } else if (handshakeType == HandshakeType.FINISHED) {

        } else {
            throw new IllegalArgumentException("Unknown handshake type " + handshakeType);
        }
    }

    @Override
    public String toString() {
        return "  Handshake type: " + handshakeType + "\n" +
                content.toString();
    }
}

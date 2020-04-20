package ru.serega6531.packmate.service.optimization.tls.records;

import org.pcap4j.util.ByteArrays;
import ru.serega6531.packmate.service.optimization.tls.numbers.HandshakeType;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.ClientHelloHandshakeRecordContent;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.HandshakeRecordContent;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.ServerHelloHandshakeRecordContent;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.UnknownRecordContent;
import ru.serega6531.packmate.utils.BytesUtils;

import static org.pcap4j.util.ByteArrays.BYTE_SIZE_IN_BYTES;

public class HandshakeRecord implements TlsRecord {

    /*
    0x0 - Handshake type
    0x1 - Handshake length
    0x4 - Handshake version
    0x6 - Handshake content
     */

    private static final int HANDSHAKE_TYPE_OFFSET = 0;
    private static final int LENGTH_OFFSET = HANDSHAKE_TYPE_OFFSET + BYTE_SIZE_IN_BYTES;
    private static final int CONTENT_OFFSET = LENGTH_OFFSET + 3;

    private HandshakeType handshakeType;
    private int handshakeLength;  // 3 bytes
    private HandshakeRecordContent content;

    public static HandshakeRecord newInstance(byte[] rawData, int offset, int length) {
        ByteArrays.validateBounds(rawData, offset, length);
        return new HandshakeRecord(rawData, offset);
    }

    private HandshakeRecord(byte[] rawData, int offset) {
        this.handshakeType = HandshakeType.getInstance(ByteArrays.getByte(rawData, HANDSHAKE_TYPE_OFFSET + offset));
        this.handshakeLength = BytesUtils.getThreeBytesInt(rawData, LENGTH_OFFSET + offset);

        if (handshakeType == HandshakeType.CLIENT_HELLO) {
            this.content = ClientHelloHandshakeRecordContent.newInstance(
                    rawData, offset + CONTENT_OFFSET, handshakeLength);
        } else if (handshakeType == HandshakeType.SERVER_HELLO) {
            this.content = ServerHelloHandshakeRecordContent.newInstance(
                    rawData, offset + CONTENT_OFFSET, handshakeLength);
        } else {
            this.content = UnknownRecordContent.newInstance(
                    rawData, offset + CONTENT_OFFSET, handshakeLength);
        }
    }

    @Override
    public String toString() {
        return "    Handshake length: " + handshakeLength + "\n" +
                "    Handshake type: " + handshakeType + "\n" +
                content.toString();
    }
}

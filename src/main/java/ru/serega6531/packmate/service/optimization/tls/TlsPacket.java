package ru.serega6531.packmate.service.optimization.tls;

import org.pcap4j.packet.AbstractPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.ByteArrays;
import ru.serega6531.packmate.service.optimization.tls.numbers.ContentType;
import ru.serega6531.packmate.service.optimization.tls.numbers.TlsVersion;
import ru.serega6531.packmate.service.optimization.tls.records.ChangeCipherSpecRecord;
import ru.serega6531.packmate.service.optimization.tls.records.HandshakeRecord;
import ru.serega6531.packmate.service.optimization.tls.records.TlsRecord;

import java.util.ArrayList;
import java.util.List;

import static org.pcap4j.util.ByteArrays.BYTE_SIZE_IN_BYTES;
import static org.pcap4j.util.ByteArrays.SHORT_SIZE_IN_BYTES;

public class TlsPacket extends AbstractPacket {

    private final TlsPacket.TlsHeader header;
    private final Packet payload;

    public static TlsPacket newPacket(byte[] rawData, int offset, int length) throws IllegalRawDataException {
        ByteArrays.validateBounds(rawData, offset, length);
        return new TlsPacket(rawData, offset, length);
    }

    private TlsPacket(byte[] rawData, int offset, int length) throws IllegalRawDataException {
        this.header = new TlsPacket.TlsHeader(rawData, offset, length);

        int payloadLength = length - header.length();
        if (payloadLength > 0) {
            this.payload = TlsPacket.newPacket(rawData, offset + header.length(), payloadLength);
        } else {
            this.payload = null;
        }
    }

    private TlsPacket(TlsPacket.Builder builder) {
        if (builder == null) {
            throw new NullPointerException("builder: null");
        }

        this.payload = builder.payloadBuilder != null ? builder.payloadBuilder.build() : null;
        this.header = new TlsPacket.TlsHeader(builder);
    }

    @Override
    public TlsHeader getHeader() {
        return header;
    }

    @Override
    public Builder getBuilder() {
        return new Builder(this);
    }

    public static final class TlsHeader extends AbstractHeader {

        private static final int CONTENT_TYPE_OFFSET = 0;
        private static final int VERSION_OFFSET = CONTENT_TYPE_OFFSET + BYTE_SIZE_IN_BYTES;
        private static final int LENGTH_OFFSET = VERSION_OFFSET + SHORT_SIZE_IN_BYTES;
        private static final int RECORD_OFFSET = LENGTH_OFFSET + SHORT_SIZE_IN_BYTES;

        private ContentType contentType;
        private TlsVersion version;
        private short length;
        private TlsRecord record;

        private TlsHeader(Builder builder) {
            //TODO
        }

        private TlsHeader(byte[] rawData, int offset, int length) throws IllegalRawDataException {
            //TODO check length
            this.contentType = ContentType.getInstance(ByteArrays.getByte(rawData, CONTENT_TYPE_OFFSET + offset));
            this.version = TlsVersion.getInstance(ByteArrays.getShort(rawData, VERSION_OFFSET + offset));
            this.length = ByteArrays.getShort(rawData, LENGTH_OFFSET + offset);

            if (contentType == ContentType.HANDSHAKE) {
                this.record = HandshakeRecord.newInstance(rawData, offset + RECORD_OFFSET, length);
            } else if (contentType == ContentType.CHANGE_CIPHER_SPEC) {
                this.record = ChangeCipherSpecRecord.newInstance(rawData, offset + RECORD_OFFSET, length);
            } else if (contentType == ContentType.APPLICATION_DATA) {

            } else if (contentType == ContentType.ALERT) {

            } else {
                throw new IllegalArgumentException("Unknown content type: " + contentType);
            }
        }

        @Override
        protected List<byte[]> getRawFields() {
            List<byte[]> rawFields = new ArrayList<>();
            rawFields.add(new byte[]{contentType.value()});
            rawFields.add(ByteArrays.toByteArray(version.value()));
            rawFields.add(ByteArrays.toByteArray(length));
            return rawFields;
        }

        @Override
        public int length() {
            return RECORD_OFFSET + length;
        }

        @Override
        protected String buildString() {
            return "TLS Header [" + length() + " bytes]\n" +
                    "  Version: " + version + "\n" +
                    "  Type: " + contentType + "\n" +
                    record.toString();
        }
    }

    public static final class Builder extends AbstractBuilder {

        private Packet.Builder payloadBuilder;

        public Builder() {
        }

        public Builder(TlsPacket packet) {
            this.payloadBuilder = packet.payload != null ? packet.payload.getBuilder() : null;
        }

        @Override
        public Packet build() {
            return new TlsPacket(this);
        }
    }
}

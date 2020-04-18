package ru.serega6531.packmate.service.optimization.tls;

import org.pcap4j.packet.AbstractPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.factory.PacketFactories;
import org.pcap4j.packet.namednumber.TcpPort;
import org.pcap4j.util.ByteArrays;

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
            this.payload =
                    PacketFactories.getFactory(Packet.class, TcpPort.class)
                            .newInstance(rawData, offset + header.length(), payloadLength);
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

        private ContentType contentType;
        private TlsVersion version;
        private short length;

        private TlsHeader(Builder builder) {
            //TODO
        }

        private TlsHeader(byte[] rawData, int offset, int length) throws IllegalRawDataException {
            this.contentType = ContentType.getInstance(ByteArrays.getByte(rawData, CONTENT_TYPE_OFFSET + offset));
            this.version = TlsVersion.getInstance(ByteArrays.getShort(rawData, VERSION_OFFSET + offset));
            this.length = ByteArrays.getShort(rawData, LENGTH_OFFSET + offset);
        }

        @Override
        protected List<byte[]> getRawFields() {
            List<byte[]> rawFields = new ArrayList<>();
            rawFields.add(new byte[] {contentType.value()});
            rawFields.add(ByteArrays.toByteArray(version.value()));
            rawFields.add(ByteArrays.toByteArray(length));
            return rawFields;
        }
    }

    public static final class Builder extends AbstractBuilder {

        private Packet.Builder payloadBuilder;

        public Builder() {}

        public Builder(TlsPacket packet) {
            this.payloadBuilder = packet.payload != null ? packet.payload.getBuilder() : null;
        }

        @Override
        public Packet build() {
            return new TlsPacket(this);
        }
    }
}

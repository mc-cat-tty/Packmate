package ru.serega6531.packmate.service.optimization.tls.records.handshakes;

import org.pcap4j.util.ByteArrays;
import ru.serega6531.packmate.service.optimization.tls.numbers.CipherSuite;
import ru.serega6531.packmate.service.optimization.tls.numbers.CompressionMethod;
import ru.serega6531.packmate.service.optimization.tls.numbers.ExtensionType;
import ru.serega6531.packmate.service.optimization.tls.numbers.TlsVersion;
import ru.serega6531.packmate.utils.BytesUtils;

import java.util.ArrayList;
import java.util.List;

import static org.pcap4j.util.ByteArrays.BYTE_SIZE_IN_BYTES;
import static org.pcap4j.util.ByteArrays.SHORT_SIZE_IN_BYTES;

public class ClientHelloHandshakeRecordContent implements HandshakeRecordContent {

    private static final int LENGTH_OFFSET = 0;
    private static final int VERSION_OFFSET = LENGTH_OFFSET + 3;
    private static final int RANDOM_OFFSET = VERSION_OFFSET + SHORT_SIZE_IN_BYTES;
    private static final int SESSION_ID_LENGTH_OFFSET = RANDOM_OFFSET + 32;
    private static final int SESSION_ID_OFFSET = SESSION_ID_LENGTH_OFFSET + BYTE_SIZE_IN_BYTES;
    private static final int CIPHER_SUITES_LENGTH_OFFSET = SESSION_ID_OFFSET;  // + sessionIdLength
    private static final int CIPHER_SUITE_OFFSET =
            CIPHER_SUITES_LENGTH_OFFSET + SHORT_SIZE_IN_BYTES; // + sessionIdLength + SHORT_SIZE_IN_BYTES*i
    private static final int COMPRESSION_METHODS_LENGTH_OFFSET = CIPHER_SUITE_OFFSET; // + sessionIdLength + cipherSuitesLength
    private static final int COMPRESSION_METHOD_OFFSET =
            COMPRESSION_METHODS_LENGTH_OFFSET + BYTE_SIZE_IN_BYTES; // + sessionIdLength + cipherSuitesLength + BYTE_SIZE_IN_BYTES*i
    private static final int EXTENSIONS_LENTH_OFFSET =
            COMPRESSION_METHOD_OFFSET; // + sessionIdLength + cipherSuitesLength + compressionMethodsLength
    private static final int EXTENSION_OFFSET = COMPRESSION_METHOD_OFFSET + SHORT_SIZE_IN_BYTES;

    private int length;   // 3 bytes
    private TlsVersion version;
    private byte[] random = new byte[32];
    private byte sessionIdLength;
    private byte[] sessionId;
    private short cipherSuitesLength;
    private List<CipherSuite> cipherSuites;
    private byte compressionMethodsLength;
    private List<CompressionMethod> compressionMethods;
    private short extensionsLength;

    public static ClientHelloHandshakeRecordContent newInstance(byte[] rawData, int offset, int length) {
        return new ClientHelloHandshakeRecordContent(rawData, offset, length);
    }

    private ClientHelloHandshakeRecordContent(byte[] rawData, int offset, int length) {
        this.length = BytesUtils.getThreeBytesInt(rawData, LENGTH_OFFSET + offset);
        this.version = TlsVersion.getInstance(ByteArrays.getShort(rawData, VERSION_OFFSET + offset));
        System.arraycopy(rawData, RANDOM_OFFSET + offset, random, 0, 32);
        this.sessionIdLength = ByteArrays.getByte(rawData, SESSION_ID_LENGTH_OFFSET + offset);
        this.sessionId = new byte[sessionIdLength];

        if (sessionIdLength != 0) {
            System.arraycopy(rawData, SESSION_ID_OFFSET + offset, sessionId, 0, sessionIdLength);
        }

        this.cipherSuitesLength = ByteArrays.getShort(rawData, CIPHER_SUITES_LENGTH_OFFSET + sessionIdLength + offset);
        int cipherSuitesAmount = cipherSuitesLength / SHORT_SIZE_IN_BYTES;
        this.cipherSuites = new ArrayList<>(cipherSuitesAmount);

        for (int i = 0; i < cipherSuitesAmount; i++) {
            this.cipherSuites.add(CipherSuite.getInstance(ByteArrays.getShort(rawData,
                    CIPHER_SUITE_OFFSET + SHORT_SIZE_IN_BYTES * i + sessionIdLength + offset)));
        }

        this.compressionMethodsLength = ByteArrays.getByte(rawData,
                COMPRESSION_METHODS_LENGTH_OFFSET + cipherSuitesLength + sessionIdLength + offset);
        this.compressionMethods = new ArrayList<>(compressionMethodsLength);

        for (byte i = 0; i < compressionMethodsLength; i++) {
            this.compressionMethods.add(CompressionMethod.getInstance(ByteArrays.getByte(rawData,
                    COMPRESSION_METHOD_OFFSET + BYTE_SIZE_IN_BYTES * i + sessionIdLength + cipherSuitesLength + offset)));
        }

        this.extensionsLength = ByteArrays.getShort(rawData,
                COMPRESSION_METHOD_OFFSET + compressionMethodsLength + sessionIdLength + cipherSuitesLength + offset);

        int cursor = EXTENSION_OFFSET + compressionMethodsLength + sessionIdLength + cipherSuitesLength + offset;
        int extensionsEnd = cursor + extensionsLength;

        while (cursor < extensionsEnd) {
            ExtensionType extensionType = ExtensionType.getInstance(ByteArrays.getShort(rawData, cursor));
            cursor += SHORT_SIZE_IN_BYTES;
            short extensionLength = ByteArrays.getShort(rawData, cursor);
            cursor += SHORT_SIZE_IN_BYTES;
            cursor += extensionLength;
            //TODO
        }
    }

    @Override
    public String toString() {
        return "    Handshake length: " + length + "\n" +
                "    TLS version: " + version + "\n" +
                "    Client random: " + ByteArrays.toHexString(random, "") + "\n" +
                "    Session id: " + (sessionIdLength > 0 ? ByteArrays.toHexString(sessionId, "") : "null") + "\n" +
                "    Cipher suites: " + cipherSuites.toString() + "\n" +
                "    Compression methods: " + compressionMethods.toString() + "\n" +
                "    Extensions: TODO";
    }
}

package ru.serega6531.packmate.service.optimization.tls.records;

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.util.ByteArrays;

public class ChangeCipherSpecRecord extends TlsRecord {

    private byte changeCipherSpecMessage;

    public static ChangeCipherSpecRecord newInstance(byte[] rawData, int offset, int length) {
        ByteArrays.validateBounds(rawData, offset, length);
        return new ChangeCipherSpecRecord(rawData, offset);
    }

    private ChangeCipherSpecRecord(byte[] rawData, int offset) {
        this.changeCipherSpecMessage = ByteArrays.getByte(rawData, offset);
    }

}

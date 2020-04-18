package ru.serega6531.packmate.service.optimization.tls.records;

public class ApplicationDataRecord extends TlsRecord {

    private byte[] data;

    public static ApplicationDataRecord newInstance(byte[] rawData, int offset, int length) {
        return new ApplicationDataRecord(rawData, offset, length);
    }

    public ApplicationDataRecord(byte[] rawData, int offset, int length) {
        data = new byte[length];
        System.arraycopy(rawData, offset, data, 0, length);
    }

    @Override
    public String toString() {
        return "  Encrypted data: [" + data.length + " bytes]";
    }

}

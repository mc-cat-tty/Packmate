package ru.serega6531.packmate.service.optimization.tls.extensions;

import ru.serega6531.packmate.service.optimization.tls.numbers.ExtensionType;

public class TlsExtension {

    private ExtensionType type;
    private short length;
    private byte[] data;  // TODO create packets for each extension

    public TlsExtension(ExtensionType type, short length, byte[] data) {
        this.type = type;
        this.length = length;
        this.data = data;
    }

    public ExtensionType getType() {
        return type;
    }

    public short getLength() {
        return length;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        if (data.length == 0) {
            return type.name();
        }

        return type.name() + " [" + data.length + " bytes]";
    }
}

package ru.serega6531.packmate.service.optimization.tls.extensions.keyshare;

import org.pcap4j.util.ByteArrays;
import ru.serega6531.packmate.service.optimization.tls.extensions.TlsExtension;
import ru.serega6531.packmate.service.optimization.tls.numbers.ExtensionType;

import java.util.ArrayList;
import java.util.List;

import static org.pcap4j.util.ByteArrays.SHORT_SIZE_IN_BYTES;

public class KeyShareExtension extends TlsExtension {

    private static final int KEY_SHARE_LENGTH_OFFSET = 0;
    private static final int KEY_SHARE_ENTRY_OFFSET = KEY_SHARE_LENGTH_OFFSET + SHORT_SIZE_IN_BYTES;

    private short keyShareLength;
    private List<KeyShareEntry> entries = new ArrayList<>();

    public KeyShareExtension(ExtensionType type, byte[] rawData, int offset, short extensionLength) {
        super(type, extensionLength);

        this.keyShareLength = ByteArrays.getShort(rawData, KEY_SHARE_LENGTH_OFFSET + offset);  // the field is not always there
        ByteArrays.validateBounds(rawData, KEY_SHARE_ENTRY_OFFSET + offset, keyShareLength);

        int cursor = KEY_SHARE_ENTRY_OFFSET + offset;

        while (cursor < offset + this.keyShareLength) {
            KeyShareEntry entry = new KeyShareEntry(rawData, cursor);
            entries.add(entry);
            cursor += entry.size();
        }
    }

    @Override
    public String toString() {
        return type.name() + " " + entries.toString();
    }
}

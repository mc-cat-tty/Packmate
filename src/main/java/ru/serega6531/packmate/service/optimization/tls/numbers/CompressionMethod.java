package ru.serega6531.packmate.service.optimization.tls.numbers;

import org.pcap4j.packet.namednumber.NamedNumber;

import java.util.HashMap;
import java.util.Map;

public class CompressionMethod extends NamedNumber<Byte, CompressionMethod> {

    public static final CompressionMethod NULL = new CompressionMethod((byte) 0, "null");

    private static final Map<Byte, CompressionMethod> registry = new HashMap<>();

    static {
        registry.put(NULL.value(), NULL);
    }

    public CompressionMethod(Byte value, String name) {
        super(value, name);
    }

    public static CompressionMethod getInstance(Byte value) {
        if (registry.containsKey(value)) {
            return registry.get(value);
        } else {
            return new CompressionMethod(value, "Unknown");
        }
    }

    @Override
    public int compareTo(CompressionMethod o) {
        return value().compareTo(o.value());
    }
}

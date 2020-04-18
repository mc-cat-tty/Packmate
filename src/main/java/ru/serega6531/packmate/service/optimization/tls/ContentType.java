package ru.serega6531.packmate.service.optimization.tls;

import org.pcap4j.packet.namednumber.NamedNumber;

import java.util.HashMap;
import java.util.Map;

public class ContentType extends NamedNumber<Byte, ContentType> {

    public static final ContentType HANDSHAKE = new ContentType((byte) 22, "Handshake");
    public static final ContentType APPLICATION_DATA = new ContentType((byte) 23, "Application Data");

    private static final Map<Byte, ContentType> registry = new HashMap<>();

    static {
        registry.put(HANDSHAKE.value(), HANDSHAKE);
        registry.put(APPLICATION_DATA.value(), APPLICATION_DATA);
    }

    public ContentType(Byte value, String name) {
        super(value, name);
    }

    public static ContentType getInstance(Byte value) {
        if (registry.containsKey(value)) {
            return registry.get(value);
        } else {
            return new ContentType(value, "unknown");
        }
    }

    @Override
    public int compareTo(ContentType o) {
        return value().compareTo(o.value());
    }
}

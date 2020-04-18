package ru.serega6531.packmate.service.optimization.tls;

import org.pcap4j.packet.namednumber.NamedNumber;

import java.util.HashMap;
import java.util.Map;

public class TlsVersion extends NamedNumber<Short, TlsVersion> {

    public static final TlsVersion TLS_1_0 = new TlsVersion((short) 0x0301, "TLS 1.0");
    public static final TlsVersion TLS_1_2 = new TlsVersion((short) 0x0303, "TLS 1.2");

    private static final Map<Short, TlsVersion> registry = new HashMap<>();

    static {
        registry.put(TLS_1_0.value(), TLS_1_0);
        registry.put(TLS_1_2.value(), TLS_1_2);
    }

    public TlsVersion(Short value, String name) {
        super(value, name);
    }

    public static TlsVersion getInstance(Short value) {
        if (registry.containsKey(value)) {
            return registry.get(value);
        } else {
            return new TlsVersion(value, "unknown");
        }
    }

    @Override
    public int compareTo(TlsVersion o) {
        return value().compareTo(o.value());
    }

}

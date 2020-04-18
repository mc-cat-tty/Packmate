package ru.serega6531.packmate.service.optimization.tls.numbers;

import org.pcap4j.packet.namednumber.NamedNumber;

import java.util.HashMap;
import java.util.Map;

public class CipherSuite extends NamedNumber<Short, CipherSuite> {

    public static final CipherSuite RESERVED_GREASE = new CipherSuite((short) 0xdada, "Reserved (GREASE)");
    public static final CipherSuite TLS_AES_128_GCM_SHA256 = new CipherSuite((short) 0x1301, "TLS_AES_128_GCM_SHA256");

    private static final Map<Short, CipherSuite> registry = new HashMap<>();

    static {
        registry.put(RESERVED_GREASE.value(), RESERVED_GREASE);
        registry.put(TLS_AES_128_GCM_SHA256.value(), TLS_AES_128_GCM_SHA256);
        //TODO add all
    }

    public CipherSuite(Short value, String name) {
        super(value, name);
    }

    public static CipherSuite getInstance(Short value) {
        if (registry.containsKey(value)) {
            return registry.get(value);
        } else {
            return new CipherSuite(value, "unknown");
        }
    }

    @Override
    public int compareTo(CipherSuite o) {
        return 0;
    }
}

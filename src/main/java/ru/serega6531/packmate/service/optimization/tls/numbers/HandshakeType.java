package ru.serega6531.packmate.service.optimization.tls.numbers;

import org.pcap4j.packet.namednumber.NamedNumber;

import java.util.HashMap;
import java.util.Map;

public class HandshakeType extends NamedNumber<Byte, HandshakeType> {

    public static final HandshakeType HELLO_REQUEST = new HandshakeType((byte) 0, "Hello Request");
    public static final HandshakeType CLIENT_HELLO = new HandshakeType((byte) 1, "Client Hello");
    public static final HandshakeType SERVER_HELLO = new HandshakeType((byte) 2, "Server Hello");
    public static final HandshakeType CERTIFICATE = new HandshakeType((byte) 11, "Certificate");
    public static final HandshakeType SERVER_KEY_EXCHANGE = new HandshakeType((byte) 12, "Server Key Excange");
    public static final HandshakeType CERTIFICATE_REQUEST = new HandshakeType((byte) 13, "Certificate Request");
    public static final HandshakeType SERVER_HELLO_DONE = new HandshakeType((byte) 14, "Server Hello Done");
    public static final HandshakeType CERTIFICATE_VERIFY = new HandshakeType((byte) 15, "Certificate Verify");
    public static final HandshakeType CLIENT_KEY_EXCHANGE = new HandshakeType((byte) 16, "Client Key Exchange");
    public static final HandshakeType FINISHED = new HandshakeType((byte) 20, "Finished");

    private static final Map<Byte, HandshakeType> registry = new HashMap<>();

    static {
        registry.put(HELLO_REQUEST.value(), HELLO_REQUEST);
        registry.put(CLIENT_HELLO.value(), CLIENT_HELLO);
        registry.put(SERVER_HELLO.value(), SERVER_HELLO);
        registry.put(CERTIFICATE.value(), CERTIFICATE);
        registry.put(SERVER_KEY_EXCHANGE.value(), SERVER_KEY_EXCHANGE);
        registry.put(CERTIFICATE_REQUEST.value(), CERTIFICATE_REQUEST);
        registry.put(SERVER_HELLO_DONE.value(), SERVER_HELLO_DONE);
        registry.put(CERTIFICATE_VERIFY.value(), CERTIFICATE_VERIFY);
        registry.put(CLIENT_KEY_EXCHANGE.value(), CLIENT_KEY_EXCHANGE);
        registry.put(FINISHED.value(), FINISHED);
    }

    public HandshakeType(Byte value, String name) {
        super(value, name);
    }

    public static HandshakeType getInstance(Byte value) {
        if (registry.containsKey(value)) {
            return registry.get(value);
        } else {
            throw new IllegalArgumentException("Unknown handshake type " + value);
        }
    }

    @Override
    public int compareTo(HandshakeType o) {
        return value().compareTo(o.value());
    }
}

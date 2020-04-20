package ru.serega6531.packmate.service.optimization.tls.keys.enums;

import java.util.HashMap;
import java.util.Map;

public enum SignatureHashAlgorithmHash {

    SHA256((byte) 4),
    SHA512((byte) 6);

    private final byte value;

    private static final Map<Byte, SignatureHashAlgorithmHash> map = new HashMap<>();

    SignatureHashAlgorithmHash(byte value) {
        this.value = value;
    }

    static {
        for (SignatureHashAlgorithmHash curve : values()) {
            map.put(curve.getValue(), curve);
        }
    }

    public byte getValue() {
        return value;
    }

    public static SignatureHashAlgorithmHash findByValue(short value) {
        return map.get(value);
    }

}

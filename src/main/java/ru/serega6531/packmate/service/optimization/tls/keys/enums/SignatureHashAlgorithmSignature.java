package ru.serega6531.packmate.service.optimization.tls.keys.enums;

import java.util.HashMap;
import java.util.Map;

public enum  SignatureHashAlgorithmSignature {

    RSA((byte) 1);

    private final byte value;

    private static final Map<Byte, SignatureHashAlgorithmSignature> map = new HashMap<>();

    SignatureHashAlgorithmSignature(byte value) {
        this.value = value;
    }

    static {
        for (SignatureHashAlgorithmSignature curve : values()) {
            map.put(curve.getValue(), curve);
        }
    }

    public byte getValue() {
        return value;
    }

    public static SignatureHashAlgorithmSignature findByValue(short value) {
        return map.get(value);
    }

}

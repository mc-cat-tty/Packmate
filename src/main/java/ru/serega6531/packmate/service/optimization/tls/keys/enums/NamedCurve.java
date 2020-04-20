package ru.serega6531.packmate.service.optimization.tls.keys.enums;

import java.util.HashMap;
import java.util.Map;

public enum NamedCurve {

    SECP256R1((short) 0x0017);

    private final short value;

    private static final Map<Short, NamedCurve> map = new HashMap<>();

    NamedCurve(short value) {
        this.value = value;
    }

    static {
        for (NamedCurve curve : values()) {
            map.put(curve.getValue(), curve);
        }
    }

    public short getValue() {
        return value;
    }

    public static NamedCurve findByValue(short value) {
        return map.get(value);
    }
}

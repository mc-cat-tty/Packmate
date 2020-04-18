package ru.serega6531.packmate.service.optimization.tls.numbers;

import org.pcap4j.packet.namednumber.NamedNumber;

import java.util.HashMap;
import java.util.Map;

public class ExtensionType extends NamedNumber<Short, ExtensionType> {

    public static final ExtensionType RESERVED_GREASE = new ExtensionType((short) 14906, "Reserved (GREASE)");
    public static final ExtensionType PADDING = new ExtensionType((short) 21, "Padding");

    private static final Map<Short, ExtensionType> registry = new HashMap<>();

    static {
        registry.put(RESERVED_GREASE.value(), RESERVED_GREASE);
        registry.put(PADDING.value(), PADDING);
        //TODO add all
    }

    public ExtensionType(Short value, String name) {
        super(value, name);
    }

    public static ExtensionType getInstance(Short value) {
        if (registry.containsKey(value)) {
            return registry.get(value);
        } else {
            return new ExtensionType(value, "Unknown");
        }
    }

    @Override
    public int compareTo(ExtensionType o) {
        return value().compareTo(o.value());
    }
}

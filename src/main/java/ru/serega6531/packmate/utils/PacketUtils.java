package ru.serega6531.packmate.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;
import ru.serega6531.packmate.model.Packet;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class PacketUtils {

    public Optional<byte[]> mergePackets(List<Packet> cut) {
        return cut.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll);
    }

}

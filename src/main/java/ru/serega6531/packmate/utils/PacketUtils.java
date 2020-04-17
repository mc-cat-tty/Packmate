package ru.serega6531.packmate.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;
import ru.serega6531.packmate.model.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class PacketUtils {

    public Optional<byte[]> mergePackets(List<Packet> cut) {
        return cut.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll);
    }

    public List<List<Packet>> sliceToSides(List<Packet> packets) {
        List<List<Packet>> result = new ArrayList<>();
        List<Packet> side = new ArrayList<>();
        boolean incoming = true;

        for (Packet packet : packets) {
            if(packet.isIncoming() != incoming) {
                incoming = packet.isIncoming();

                if(!side.isEmpty()) {
                    result.add(side);
                    side = new ArrayList<>();
                }
            }

            side.add(packet);
        }

        if(!side.isEmpty()) {
            result.add(side);
        }

        return result;
    }

}

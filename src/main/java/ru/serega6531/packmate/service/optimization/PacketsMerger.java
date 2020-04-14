package ru.serega6531.packmate.service.optimization;

import lombok.AllArgsConstructor;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.PacketUtils;

import java.util.List;

@AllArgsConstructor
public class PacketsMerger {

    private final List<Packet> packets;

    /**
     * Сжать соседние пакеты в одном направлении в один.
     * Выполняется после других оптимизаций чтобы правильно определять границы пакетов.
     */
    public void mergeAdjacentPackets() {
        int start = 0;
        int packetsInRow = 0;
        boolean incoming = true;

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);
            if (packet.isIncoming() != incoming) {
                if (packetsInRow > 1) {
                    compress(start, i);

                    i = start + 1;  // продвигаем указатель на следующий после склеенного блок
                }
                start = i;
                packetsInRow = 1;
            } else {
                packetsInRow++;
            }

            incoming = packet.isIncoming();
        }

        if (packetsInRow > 1) {
            compress(start, packets.size());
        }
    }

    /**
     * Сжать кусок со start по end в один пакет
     */
    private void compress(int start, int end) {
        final List<Packet> cut = packets.subList(start, end);
        final long timestamp = cut.get(0).getTimestamp();
        final boolean ungzipped = cut.stream().anyMatch(Packet::isUngzipped);
        final boolean webSocketParsed = cut.stream().anyMatch(Packet::isWebSocketParsed);
        boolean incoming = cut.get(0).isIncoming();
        //noinspection OptionalGetWithoutIsPresent
        final byte[] content = PacketUtils.mergePackets(cut).get();

        packets.removeAll(cut);
        packets.add(start, Packet.builder()
                .incoming(incoming)
                .timestamp(timestamp)
                .ungzipped(ungzipped)
                .webSocketParsed(webSocketParsed)
                .content(content)
                .build());
    }

}

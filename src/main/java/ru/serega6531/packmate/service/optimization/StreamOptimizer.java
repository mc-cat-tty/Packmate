package ru.serega6531.packmate.service.optimization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Packet;

import java.util.List;

@AllArgsConstructor
@Slf4j
public class StreamOptimizer {

    private final CtfService service;
    private List<Packet> packets;

    private final PacketsMerger merger = new PacketsMerger();
    private final HttpUrldecodeProcessor urldecodeProcessor = new HttpUrldecodeProcessor();
    private final HttpProcessor httpProcessor = new HttpProcessor();


    /**
     * Вызвать для выполнения оптимизаций на переданном списке пакетов.
     */
    public List<Packet> optimizeStream() {
        if (service.isParseWebSockets()) {
            try {
                parseWebSockets();
            } catch (Exception e) {
                log.warn("Error optimizing stream (websockets)", e);
                return packets;
            }
        }

        if (service.isUrldecodeHttpRequests()) {
            try {
                urldecodeProcessor.urldecodeRequests(packets);
            } catch (Exception e) {
                log.warn("Error optimizing stream (urldecode)", e);
                return packets;
            }
        }

        if (service.isMergeAdjacentPackets() || service.isHttp()) {
            try {
                merger.mergeAdjacentPackets(packets);
            } catch (Exception e) {
                log.warn("Error optimizing stream (adjacent)", e);
                return packets;
            }
        }

        if (service.isHttp()) {
            try {
                httpProcessor.process(packets);
            } catch (Exception e) {
                log.warn("Error optimizing stream (http)", e);
                return packets;
            }
        }

        return packets;
    }

    private void parseWebSockets() {
        if (!packets.get(0).getContentString().contains("HTTP/")) {
            return;
        }

        final WebSocketsParser parser = new WebSocketsParser(packets);
        if (!parser.isParsed()) {
            return;
        }

        packets = parser.getParsedPackets();
    }

}

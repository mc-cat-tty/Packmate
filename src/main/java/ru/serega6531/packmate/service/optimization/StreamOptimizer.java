package ru.serega6531.packmate.service.optimization;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Packet;

import java.util.List;

@AllArgsConstructor
@Slf4j
public class StreamOptimizer {

    private final RsaKeysHolder keysHolder;
    private final CtfService service;
    private List<Packet> packets;

    /**
     * Вызвать для выполнения оптимизаций на переданном списке пакетов.
     */
    public List<Packet> optimizeStream() {
        if (service.isDecryptTls()) {
            try {
                decryptTls();
            } catch (Exception e) {
                log.warn("Error optimizing stream (tls)", e);
                return packets;
            }
        }

        if (service.isProcessChunkedEncoding()) {
            try {
                processChunkedEncoding();
            } catch (Exception e) {
                log.warn("Error optimizing stream (chunks)", e);
                return packets;
            }
        }

        if (service.isUngzipHttp()) {
            try {
                unpackGzip();
            } catch (Exception e) {
                log.warn("Error optimizing stream (gzip)", e);
                return packets;
            }
        }

        if (service.isParseWebSockets()) {
            try {
                parseWebSockets();
            } catch (Exception e) {
                log.warn("Error optimizing stream (websocketss)", e);
                return packets;
            }
        }

        if (service.isUrldecodeHttpRequests()) {
            try {
                urldecodeRequests();
            } catch (Exception e) {
                log.warn("Error optimizing stream (urldecode)", e);
                return packets;
            }
        }

        if (service.isMergeAdjacentPackets()) {
            try {
                mergeAdjacentPackets();
            } catch (Exception e) {
                log.warn("Error optimizing stream (adjacent)", e);
                return packets;
            }
        }

        return packets;
    }

    private void decryptTls() {
        final TlsDecryptor tlsDecryptor = new TlsDecryptor(packets, keysHolder);
        tlsDecryptor.decryptTls();

        if (tlsDecryptor.isParsed()) {
            packets = tlsDecryptor.getParsedPackets();
        }
    }

    /**
     * Сжать соседние пакеты в одном направлении в один.
     * Выполняется после других оптимизаций чтобы правильно определять границы пакетов.
     */
    private void mergeAdjacentPackets() {
        final PacketsMerger merger = new PacketsMerger(packets);
        merger.mergeAdjacentPackets();
    }

    /**
     * Декодирование urlencode с http пакета до смены стороны или окончания стрима
     */
    @SneakyThrows
    private void urldecodeRequests() {
        final HttpUrldecodeProcessor processor = new HttpUrldecodeProcessor(packets);
        processor.urldecodeRequests();
    }

    /**
     * <a href="https://ru.wikipedia.org/wiki/Chunked_transfer_encoding">Chunked transfer encoding</a>
     */
    private void processChunkedEncoding() {
        HttpChunksProcessor processor = new HttpChunksProcessor(packets);
        processor.processChunkedEncoding();
    }

    /**
     * Попытаться распаковать GZIP из исходящих http пакетов. <br>
     * GZIP поток начинается на найденном HTTP пакете с заголовком Content-Encoding: gzip
     * (при этом заголовок HTTP может быть в другом пакете)<br>
     * Поток заканчивается при обнаружении нового HTTP заголовка,
     * при смене стороны передачи или при окончании всего стрима
     */
    private void unpackGzip() {
        final HttpGzipProcessor processor = new HttpGzipProcessor(packets);
        processor.unpackGzip();
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

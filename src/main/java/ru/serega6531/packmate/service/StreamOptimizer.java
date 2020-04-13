package ru.serega6531.packmate.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.Bytes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

@AllArgsConstructor
@Slf4j
public class StreamOptimizer {

    private final CtfService service;
    private List<Packet> packets;

    private static final byte[] GZIP_HEADER = {0x1f, (byte) 0x8b, 0x08};

    /**
     * Вызвать для выполнения оптимизаций на переданном списке пакетов.
     */
    public List<Packet> optimizeStream() {
        if (service.isProcessChunkedEncoding()) {
            processChunkedEncoding();
        }

        if (service.isUngzipHttp()) {
            unpackGzip();
        }

        if (service.isParseWebSockets()) {
            parseWebSockets();
        }

        if (service.isUrldecodeHttpRequests()) {
            urldecodeRequests();
        }

        if (service.isMergeAdjacentPackets()) {
            mergeAdjacentPackets();
        }

        return packets;
    }

    /**
     * Сжать соседние пакеты в одном направлении в один.
     * Выполняется после других оптимизаций чтобы правильно определять границы пакетов.
     */
    private void mergeAdjacentPackets() {
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
        final byte[] content = mergePackets(cut).get();

        packets.removeAll(cut);
        packets.add(start, Packet.builder()
                .incoming(incoming)
                .timestamp(timestamp)
                .ungzipped(ungzipped)
                .webSocketParsed(webSocketParsed)
                .content(content)
                .build());
    }

    /**
     * Декодирование urlencode с http пакета до смены стороны или окончания стрима
     */
    @SneakyThrows
    private void urldecodeRequests() {
        boolean httpStarted = false;

        for (Packet packet : packets) {
            if (packet.isIncoming()) {
                String content = packet.getContentString();
                if (content.contains("HTTP/")) {
                    httpStarted = true;
                }

                if (httpStarted) {
                    try {
                        content = URLDecoder.decode(content, StandardCharsets.UTF_8.toString());
                        packet.setContent(content.getBytes());
                    } catch (IllegalArgumentException e) {
                        log.warn("urldecode", e);
                    }
                }
            } else {
                httpStarted = false;
            }
        }
    }

    private void processChunkedEncoding() {
        boolean chunkStarted = false;
        int start = -1;
        List<Packet> chunk = new ArrayList<>();

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);
            if (!packet.isIncoming()) {
                String content = packet.getContentString();

                boolean http = content.startsWith("HTTP/");
                int contentPos = content.indexOf("\r\n\r\n");

                if (http && contentPos != -1) {   // начало body
                    String headers = content.substring(0, contentPos + 2);  // захватываем первые \r\n
                    boolean chunked = headers.contains("Transfer-Encoding: chunked\r\n");
                    if (chunked) {
                        chunkStarted = true;
                        start = i;
                        chunk.add(packet);

                        if (checkCompleteChunk(chunk, start)) {
                            chunkStarted = false;
                            chunk.clear();
                        }
                    } else {
                        chunkStarted = false;
                        chunk.clear();
                    }
                } else if (chunkStarted) {
                    chunk.add(packet);
                    if (checkCompleteChunk(chunk, start)) {
                        chunkStarted = false;
                        chunk.clear();
                    }
                }
            }
        }
    }

    /**
     * @return true если чанк завершен
     */
    private boolean checkCompleteChunk(List<Packet> chunk, int start) {
        boolean end = chunk.get(chunk.size() - 1).getContentString().endsWith("\r\n0\r\n\r\n");

        if (end) {
            //noinspection OptionalGetWithoutIsPresent
            final byte[] content = mergePackets(chunk).get();

            ByteArrayOutputStream output = new ByteArrayOutputStream(content.length);

            final int contentStart = Bytes.indexOf(content, "\r\n\r\n".getBytes()) + 4;
            output.write(content, 0, contentStart);

            final byte[] body = Arrays.copyOfRange(content, contentStart, content.length);

            int currentPos = 0;

            while (true) {
                final String found = readChunkSize(body, currentPos);
                if (found != null) {
                    final int chunkSize = Integer.parseInt(found, 16);

                    if (chunkSize == 0) {  // конец потока чанков
                        output.write('\r');
                        output.write('\n');

                        Packet result = Packet.builder()
                                .incoming(false)
                                .timestamp(chunk.get(0).getTimestamp())
                                .ungzipped(false)
                                .webSocketParsed(false)
                                .content(output.toByteArray())
                                .build();

                        packets.removeAll(chunk);
                        packets.add(start, result);

                        return true;
                    }

                    currentPos += found.length() + 2;

                    if (currentPos + chunkSize >= body.length) {
                        log.warn("Failed to merge chunks, chunk size too big: {} + {} > {}", currentPos, chunkSize, body.length);
                        return true;  // обнулить список, но не заменять пакеты
                    }

                    output.write(body, currentPos, chunkSize);
                    currentPos += chunkSize;

                    if (currentPos + 2 >= body.length || body[currentPos] != '\r' || body[currentPos + 1] != '\n') {
                        log.warn("Failed to merge chunks, chunk doesn't end with \\r\\n");
                        return true;  // обнулить список, но не заменять пакеты
                    }

                    currentPos += 2;
                } else {
                    log.warn("Failed to merge chunks, next chunk size not found");
                    return true;  // обнулить список, но не заменять пакеты
                }
            }
        }

        return false;
    }

    private String readChunkSize(byte[] content, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < content.length - 1; i++) {
            byte b = content[i];

            if ((b >= '0' && b <= '9') || (b >= 'a' && b <= 'f')) {
                sb.append((char) b);
            } else if (b == '\r' && content[i + 1] == '\n') {
                return sb.toString();
            }
        }

        return null;
    }

    /**
     * Попытаться распаковать GZIP из исходящих http пакетов. <br>
     * GZIP поток начинается на найденном HTTP пакете с заголовком Content-Encoding: gzip
     * (при этом заголовок HTTP может быть в другом пакете)<br>
     * Поток заканчивается при обнаружении нового HTTP заголовка,
     * при смене стороны передачи или при окончании всего стрима
     */
    private void unpackGzip() {
        boolean gzipStarted = false;
        int gzipStartPacket = 0;
        int gzipEndPacket;

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);

            if (packet.isIncoming() && gzipStarted) {   // поток gzip закончился
                gzipEndPacket = i - 1;
                if (extractGzip(gzipStartPacket, gzipEndPacket)) {
                    gzipStarted = false;
                    i = gzipStartPacket + 1;  // продвигаем указатель на следующий после склеенного блок
                }
            } else if (!packet.isIncoming()) {
                String content = packet.getContentString();

                int contentPos = content.indexOf("\r\n\r\n");
                boolean http = content.startsWith("HTTP/");

                if (http && gzipStarted) {  // начался новый http пакет, заканчиваем старый gzip поток
                    gzipEndPacket = i - 1;
                    if (extractGzip(gzipStartPacket, gzipEndPacket)) {
                        gzipStarted = false;
                        i = gzipStartPacket + 1;  // продвигаем указатель на следующий после склеенного блок
                    }
                }

                if (contentPos != -1) {   // начало body
                    String headers = content.substring(0, contentPos + 2);  // захватываем первые \r\n
                    boolean gziped = headers.contains("Content-Encoding: gzip\r\n");
                    if (gziped) {
                        gzipStarted = true;
                        gzipStartPacket = i;
                    }
                }
            }
        }

        if (gzipStarted) {  // стрим закончился gzip пакетом
            extractGzip(gzipStartPacket, packets.size() - 1);
        }
    }

    /**
     * Попытаться распаковать кусок пакетов с gzip body и вставить результат на их место
     *
     * @return получилось ли распаковать
     */
    private boolean extractGzip(int gzipStartPacket, int gzipEndPacket) {
        List<Packet> cut = packets.subList(gzipStartPacket, gzipEndPacket + 1);

        Packet decompressed = decompressGzipPackets(cut);
        if (decompressed != null) {
            packets.removeAll(cut);
            packets.add(gzipStartPacket, decompressed);
            return true;
        }

        return false;
    }

    private Packet decompressGzipPackets(List<Packet> cut) {
        //noinspection OptionalGetWithoutIsPresent
        final byte[] content = mergePackets(cut).get();

        final int gzipStart = Bytes.indexOf(content, GZIP_HEADER);
        byte[] httpHeader = Arrays.copyOfRange(content, 0, gzipStart);
        byte[] gzipBytes = Arrays.copyOfRange(content, gzipStart, content.length);

        try {
            final GZIPInputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(gzipBytes));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(gzipStream, out);
            byte[] newContent = ArrayUtils.addAll(httpHeader, out.toByteArray());

            log.debug("GZIP decompressed: {} -> {} bytes", gzipBytes.length, out.size());

            return Packet.builder()
                    .incoming(false)
                    .timestamp(cut.get(0).getTimestamp())
                    .ungzipped(true)
                    .webSocketParsed(false)
                    .content(newContent)
                    .build();
        } catch (ZipException e) {
            log.warn("Failed to decompress gzip, leaving as it is", e);
        } catch (IOException e) {
            log.error("decompress gzip", e);
        }

        return null;
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

    private Optional<byte[]> mergePackets(List<Packet> cut) {
        return cut.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll);
    }

}

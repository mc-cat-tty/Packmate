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
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

@AllArgsConstructor
@Slf4j
public class StreamOptimizer {

    private final CtfService service;
    private final List<Packet> packets;

    private static final byte[] GZIP_HEADER = {0x1f, (byte) 0x8b, 0x08};

    /**
     * Вызвать для выполнения оптимизаций на переданном списке пакетов.
     */
    public void optimizeStream() {
        if (service.isUngzipHttp()) {
            unpackGzip();
        }

        if (service.isUrldecodeHttpRequests()) {
            urldecodeRequests();
        }

        if (service.isMergeAdjacentPackets()) {
            mergeAdjacentPackets();
        }
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
        boolean incoming = cut.get(0).isIncoming();
        //noinspection OptionalGetWithoutIsPresent
        final byte[] content = cut.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll)
                .get();

        packets.removeAll(cut);
        packets.add(start, Packet.builder()
                .incoming(incoming)
                .timestamp(timestamp)
                .ungzipped(ungzipped)
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
                String content = new String(packet.getContent());
                if (content.contains("HTTP/")) {
                    httpStarted = true;
                }

                if (httpStarted) {
                    content = URLDecoder.decode(content, StandardCharsets.UTF_8.toString());
                    packet.setContent(content.getBytes());
                }
            } else {
                httpStarted = false;
            }
        }
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
                String content = new String(packet.getContent());

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
                    String headers = content.substring(0, contentPos);
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
        final byte[] content = cut.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll)
                .get();

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
                    .content(newContent)
                    .build();
        } catch (ZipException e) {
            log.warn("Failed to decompress gzip, leaving as it is", e);
        } catch (IOException e) {
            log.error("decompress gzip", e);
        }

        return null;
    }

}

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

    public void optimizeStream() {
        if (service.isUngzipHttp()) {
            unpackGzip(packets);
        }

        if (service.isUrldecodeHttpRequests()) {
            urldecodeRequests(packets);
        }

        if (service.isMergeAdjacentPackets()) {
            mergeAdjacentPackets(packets);
        }
    }

    private void mergeAdjacentPackets(List<Packet> packets) {
        int start = 0;
        int packetsInRow = 0;
        boolean incoming = true;

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);
            if (packet.isIncoming() != incoming) {
                if (packetsInRow > 1) {
                    final List<Packet> cut = packets.subList(start, i);
                    compress(packets, cut, incoming);

                    i++;  // продвигаем указатель на следующий после склеенного блок
                }
                start = i;
                packetsInRow = 1;
            } else {
                packetsInRow++;
            }

            incoming = packet.isIncoming();
        }

        if (packetsInRow > 1) {
            final List<Packet> cut = packets.subList(start, packets.size());
            compress(packets, cut, incoming);
        }
    }

    private void compress(List<Packet> packets, List<Packet> cut, boolean incoming) {
        final long timestamp = cut.get(0).getTimestamp();
        final boolean ungzipped = cut.stream().anyMatch(Packet::isUngzipped);
        //noinspection OptionalGetWithoutIsPresent
        final byte[] content = cut.stream()
                .map(Packet::getContent)
                .reduce(ArrayUtils::addAll)
                .get();

        packets.removeAll(cut);
        packets.add(Packet.builder()
                .incoming(incoming)
                .timestamp(timestamp)
                .ungzipped(ungzipped)
                .content(content)
                .build());
    }

    @SneakyThrows
    private void urldecodeRequests(List<Packet> packets) {
        boolean httpStarted = false;

        for (Packet packet : packets) {
            if (packet.isIncoming()) {
                String content = new String(packet.getContent());
                if (content.startsWith("HTTP/")) {
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
     * Попытаться распаковать gzip из исходящих http пакетов
     */
    private void unpackGzip(List<Packet> packets) {
        boolean gzipStarted = false;
        int gzipStartPacket = 0;
        int gzipEndPacket;

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);

            if (packet.isIncoming() && gzipStarted) {   // поток gzip закончился
                gzipEndPacket = i - 1;
                if(extractGzip(packets, gzipStartPacket, gzipEndPacket)) {
                    gzipStarted = false;
                    i = gzipStartPacket + 1;  // продвигаем указатель на следующий после склеенного блок
                }
            } else if (!packet.isIncoming()) {
                String content = new String(packet.getContent());

                int contentPos = content.indexOf("\r\n\r\n");
                boolean http = content.startsWith("HTTP/");

                if (http && gzipStarted) {  // начался новый http пакет, заканчиваем старый gzip поток
                    gzipEndPacket = i - 1;
                    if(extractGzip(packets, gzipStartPacket, gzipEndPacket)) {
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
            extractGzip(packets, gzipStartPacket, packets.size() - 1);
        }
    }

    /**
     * @return получилось ли распаковать
     */
    private boolean extractGzip(List<Packet> packets, int gzipStartPacket, int gzipEndPacket) {
        List<Packet> cut = packets.subList(gzipStartPacket, gzipEndPacket + 1);

        Packet decompressed = decompressGzipPackets(cut);
        if (decompressed != null) {
            packets.removeAll(cut);
            packets.add(gzipStartPacket, decompressed);
            return true;
        }

        return false;
    }

    private Packet decompressGzipPackets(List<Packet> packets) {
        //noinspection OptionalGetWithoutIsPresent
        final byte[] content = packets.stream()
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

            log.debug("Разархивирован gzip: {} -> {} байт", gzipBytes.length, out.size());

            return Packet.builder()
                    .incoming(false)
                    .timestamp(packets.get(0).getTimestamp())
                    .ungzipped(true)
                    .content(newContent)
                    .build();
        } catch (ZipException e) {
            log.warn("Не удалось разархивировать gzip, оставляем как есть", e);
        } catch (IOException e) {
            log.error("decompress gzip", e);
        }

        return null;
    }

}

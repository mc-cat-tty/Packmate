package ru.serega6531.packmate.service.optimization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.Bytes;
import ru.serega6531.packmate.utils.PacketUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

@Slf4j
@AllArgsConstructor
public class HttpGzipProcessor {

    private static final byte[] GZIP_HEADER = {0x1f, (byte) 0x8b, 0x08};

    private List<Packet> packets;

    /**
     * Попытаться распаковать GZIP из исходящих http пакетов. <br>
     * GZIP поток начинается на найденном HTTP пакете с заголовком Content-Encoding: gzip
     * (при этом заголовок HTTP может быть в другом пакете)<br>
     * Поток заканчивается при обнаружении нового HTTP заголовка,
     * при смене стороны передачи или при окончании всего стрима
     */
    public void unpackGzip() {
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
        final byte[] content = PacketUtils.mergePackets(cut).get();

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
            log.warn("Failed to decompress gzip, leaving as it is: {}", e.getMessage());
        } catch (IOException e) {
            log.error("decompress gzip", e);
        }

        return null;
    }

}

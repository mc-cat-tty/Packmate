package ru.serega6531.packmate.service.optimization;

import com.google.common.primitives.Bytes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.PacketUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

@Slf4j
@RequiredArgsConstructor
public class HttpGzipProcessor {

    private static final String GZIP_HTTP_HEADER = "content-encoding: gzip\r\n";
    private static final byte[] GZIP_HEADER = {0x1f, (byte) 0x8b, 0x08};

    private final List<Packet> packets;

    boolean gzipStarted = false;
    private int position;

    /**
     * Попытаться распаковать GZIP из исходящих http пакетов. <br>
     * GZIP поток начинается на найденном HTTP пакете с заголовком Content-Encoding: gzip
     * (при этом заголовок HTTP может быть в другом пакете)<br>
     * Поток заканчивается при обнаружении нового HTTP заголовка,
     * при смене стороны передачи или при окончании всего стрима
     */
    public void unpackGzip() {
        int gzipStartPacket = 0;

        for (position = 0; position < packets.size(); position++) {
            Packet packet = packets.get(position);

            if (packet.isIncoming() && gzipStarted) {   // поток gzip закончился
                extractGzip(gzipStartPacket, position - 1);
            } else if (!packet.isIncoming()) {
                String content = packet.getContentString();

                int contentPos = content.indexOf("\r\n\r\n");
                boolean http = content.startsWith("HTTP/");

                if (http && gzipStarted) {  // начался новый http пакет, заканчиваем старый gzip поток
                    extractGzip(gzipStartPacket, position - 1);
                }

                if (contentPos != -1) {   // начало body
                    String headers = content.substring(0, contentPos + 2);  // захватываем первые \r\n
                    boolean gziped = headers.toLowerCase().contains(GZIP_HTTP_HEADER);
                    if (gziped) {
                        gzipStarted = true;
                        gzipStartPacket = position;
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
     */
    private void extractGzip(int gzipStartPacket, int gzipEndPacket) {
        List<Packet> cut = packets.subList(gzipStartPacket, gzipEndPacket + 1);

        Packet decompressed = decompressGzipPackets(cut);
        if (decompressed != null) {
            packets.removeAll(cut);
            packets.add(gzipStartPacket, decompressed);

            gzipStarted = false;
            position = gzipStartPacket + 1;  // продвигаем указатель на следующий после склеенного блок
        }
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
                    .tlsDecrypted(cut.get(0).isTlsDecrypted())
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

package ru.serega6531.packmate.service.optimization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.Bytes;
import ru.serega6531.packmate.utils.PacketUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class HttpChunksProcessor {

    private final List<Packet> packets;

    public void processChunkedEncoding() {
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
                            i = start + 1;
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
                        i = start + 1;
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
            final byte[] content = PacketUtils.mergePackets(chunk).get();

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

}

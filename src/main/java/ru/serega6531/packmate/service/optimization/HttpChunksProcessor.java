package ru.serega6531.packmate.service.optimization;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.Bytes;
import ru.serega6531.packmate.utils.PacketUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class HttpChunksProcessor {

    private final List<Packet> packets;

    private int position;
    private boolean chunkStarted = false;
    private final List<Packet> chunkPackets = new ArrayList<>();

    public void processChunkedEncoding() {
        int start = -1;

        for (position = 0; position < packets.size(); position++) {
            Packet packet = packets.get(position);
            if (!packet.isIncoming()) {
                String content = packet.getContentString();

                boolean http = content.startsWith("HTTP/");
                int contentPos = content.indexOf("\r\n\r\n");

                if (http && contentPos != -1) {   // начало body
                    String headers = content.substring(0, contentPos + 2);  // захватываем первые \r\n
                    boolean chunked = headers.contains("Transfer-Encoding: chunked\r\n");
                    if (chunked) {
                        chunkStarted = true;
                        start = position;
                        chunkPackets.add(packet);

                        checkCompleteChunk(chunkPackets, start);
                    } else {
                        chunkStarted = false;
                        chunkPackets.clear();
                    }
                } else if (chunkStarted) {
                    chunkPackets.add(packet);
                    checkCompleteChunk(chunkPackets, start);
                }
            }
        }
    }

    private void checkCompleteChunk(List<Packet> packets, int start) {
        boolean end = packets.get(packets.size() - 1).getContentString().endsWith("\r\n0\r\n\r\n");

        if (end) {
            processChunk(packets, start);
        }
    }

    @SneakyThrows
    private void processChunk(List<Packet> packets, int start) {
        //noinspection OptionalGetWithoutIsPresent
        final byte[] content = PacketUtils.mergePackets(packets).get();

        ByteArrayOutputStream output = new ByteArrayOutputStream(content.length);

        final int contentStart = Bytes.indexOf(content, "\r\n\r\n".getBytes()) + 4;
        output.write(content, 0, contentStart);

        ByteBuffer buf = ByteBuffer.wrap(Arrays.copyOfRange(content, contentStart, content.length));

        while (true) {
            final String found = readChunkSize(buf);
            if (found != null) {
                final int chunkSize = Integer.parseInt(found, 16);

                if (chunkSize == 0) {  // конец потока чанков
                    Packet result = Packet.builder()
                            .incoming(false)
                            .timestamp(packets.get(0).getTimestamp())
                            .ungzipped(false)
                            .webSocketParsed(false)
                            .content(output.toByteArray())
                            .build();

                    this.packets.removeAll(packets);
                    this.packets.add(start, result);

                    resetChunk();
                    position = start + 1;

                    return;
                }

                if (chunkSize > buf.remaining()) {
                    log.warn("Failed to merge chunks, chunk size too big: {} + {} > {}",
                            buf.position(), chunkSize, buf.capacity());
                    resetChunk();
                    return;
                }

                byte[] chunk = new byte[chunkSize];
                buf.get(chunk);
                output.write(chunk);

                if (buf.remaining() < 2) {
                    log.warn("Failed to merge chunks, chunk doesn't end with \\r\\n");
                    resetChunk();
                    return;
                }

                int c1 = buf.get();
                int c2 = buf.get();
                if(c1 != '\r' || c2 != '\n') {
                    log.warn("Failed to merge chunks, chunk trailer is not equal to \\r\\n");
                    resetChunk();
                    return;
                }
            } else {
                log.warn("Failed to merge chunks, next chunk size not found");
                resetChunk();
                return;
            }
        }
    }

    private void resetChunk() {
        chunkStarted = false;
        chunkPackets.clear();
    }

    private String readChunkSize(ByteBuffer buf) {
        StringBuilder sb = new StringBuilder();

        while (buf.remaining() > 2) {
            byte b = buf.get();

            if ((b >= '0' && b <= '9') || (b >= 'a' && b <= 'f')) {
                sb.append((char) b);
            } else if (b == '\r') {
                if(buf.get() == '\n') {
                    return sb.toString();
                } else {
                    return null; // после \r не идет \n
                }
            }
        }

        return null;
    }

}

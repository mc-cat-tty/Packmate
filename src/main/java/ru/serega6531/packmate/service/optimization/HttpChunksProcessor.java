package ru.serega6531.packmate.service.optimization;

import com.google.common.primitives.Bytes;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.BytesUtils;
import ru.serega6531.packmate.utils.PacketUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        boolean end = Arrays.equals(packets.get(packets.size() - 1).getContent(), "0\r\n\r\n".getBytes()) ||
                BytesUtils.endsWith(packets.get(packets.size() - 1).getContent(), "\r\n0\r\n\r\n".getBytes());

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
            final int chunkSize = readChunkSize(buf);

            switch (chunkSize) {
                case -1 -> {
                    log.warn("Failed to merge chunks, next chunk size not found");
                    resetChunk();
                    return;
                }
                case 0 -> {
                    buildWholePacket(packets, start, output);
                    return;
                }
                default -> {
                    if (!readChunk(buf, chunkSize, output)) return;
                    if (!readTrailer(buf)) return;
                }
            }
        }
    }

    private boolean readChunk(ByteBuffer buf, int chunkSize, ByteArrayOutputStream output) throws IOException {
        if (chunkSize > buf.remaining()) {
            log.warn("Failed to merge chunks, chunk size too big: {} + {} > {}",
                    buf.position(), chunkSize, buf.capacity());
            resetChunk();
            return false;
        }

        byte[] chunk = new byte[chunkSize];
        buf.get(chunk);
        output.write(chunk);
        return true;
    }

    private boolean readTrailer(ByteBuffer buf) {
        if (buf.remaining() < 2) {
            log.warn("Failed to merge chunks, chunk doesn't end with \\r\\n");
            resetChunk();
            return false;
        }

        int c1 = buf.get();
        int c2 = buf.get();

        if (c1 != '\r' || c2 != '\n') {
            log.warn("Failed to merge chunks, chunk trailer is not equal to \\r\\n");
            resetChunk();
            return false;
        }

        return true;
    }

    private void buildWholePacket(List<Packet> packets, int start, ByteArrayOutputStream output) {
        Packet result = Packet.builder()
                .incoming(false)
                .timestamp(packets.get(0).getTimestamp())
                .ungzipped(false)
                .webSocketParsed(false)
                .tlsDecrypted(packets.get(0).isTlsDecrypted())
                .content(output.toByteArray())
                .build();

        this.packets.removeAll(packets);
        this.packets.add(start, result);

        resetChunk();
        position = start + 1;
    }

    private void resetChunk() {
        chunkStarted = false;
        chunkPackets.clear();
    }

    private int readChunkSize(ByteBuffer buf) {
        StringBuilder sb = new StringBuilder();

        while (buf.remaining() > 2) {
            byte b = buf.get();

            if ((b >= '0' && b <= '9') || (b >= 'a' && b <= 'f')) {
                sb.append((char) b);
            } else if (b == '\r') {
                if (buf.get() == '\n') {
                    return Integer.parseInt(sb.toString(), 16);
                } else {
                    return -1; // после \r не идет \n
                }
            } else {
                return -1;
            }
        }

        return -1;
    }

}

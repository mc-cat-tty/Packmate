package ru.serega6531.packmate;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.service.StreamOptimizer;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamOptimizerTest {

    @Test
    void testUnpackGzip() {
        String encoded = "H4sIAAAAAAAA/0tMTExKSgIA2KWG6gYAAAA=";
        final byte[] gzipped = Base64.getDecoder().decode(encoded);
        final byte[] content = ArrayUtils.addAll("HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Length: 26\r\n\r\n".getBytes(), gzipped);

        CtfService service = new CtfService();
        service.setUngzipHttp(true);

        Packet p = createPacket(content, false);
        List<Packet> list = new ArrayList<>();
        list.add(p);

        list = new StreamOptimizer(service, list).optimizeStream();
        final String processed = list.get(0).getContentString();
        assertTrue(processed.contains("aaabbb"));
    }

    @Test
    void testUrldecodeRequests() {
        CtfService service = new CtfService();
        service.setUrldecodeHttpRequests(true);

        Packet p = createPacket("GET /?q=%D0%B0+%D0%B1 HTTP/1.1\r\n\r\n".getBytes(), true);
        List<Packet> list = new ArrayList<>();
        list.add(p);

        list = new StreamOptimizer(service, list).optimizeStream();
        final String processed = list.get(0).getContentString();
        assertTrue(processed.contains("а б"));
    }

    @Test
    void testMergeAdjacentPackets() {
        CtfService service = new CtfService();
        service.setMergeAdjacentPackets(true);

        Packet p1 = createPacket(1, false);
        Packet p2 = createPacket(2, true);
        Packet p3 = createPacket(3, true);
        Packet p4 = createPacket(4, false);
        Packet p5 = createPacket(5, true);
        Packet p6 = createPacket(6, true);

        List<Packet> list = new ArrayList<>();
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p6);

        list = new StreamOptimizer(service, list).optimizeStream();

        assertEquals(4, list.size());
        assertEquals(2, list.get(1).getContent().length);
        assertEquals(1, list.get(2).getContent().length);
        assertEquals(2, list.get(3).getContent().length);
    }

    private Packet createPacket(int content, boolean incoming) {
        return createPacket(new byte[] {(byte) content}, incoming);
    }

    private Packet createPacket(byte[] content, boolean incoming) {
        Packet p = new Packet();
        p.setContent(content);
        p.setIncoming(incoming);
        return p;
    }

}
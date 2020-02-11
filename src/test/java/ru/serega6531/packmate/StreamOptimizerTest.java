package ru.serega6531.packmate;

import org.junit.jupiter.api.Test;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.service.StreamOptimizer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StreamOptimizerTest {

    @Test
    public void testUrldecodeRequests() {
        CtfService service = new CtfService();
        service.setUrldecodeHttpRequests(true);

        Packet p = createPacket("GET /?q=%D0%B0+%D0%B1 HTTP/1.1\r\n\r\n".getBytes(), true);
        List<Packet> list = new ArrayList<>();
        list.add(p);

        new StreamOptimizer(service, list).optimizeStream();
        final String processed = new String(list.get(0).getContent());
        assertTrue(processed.contains("а б"));
    }

    @Test
    public void testMergeAdjacentPackets() {
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

        new StreamOptimizer(service, list).optimizeStream();

        assertEquals(4, list.size());
        //TODO
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
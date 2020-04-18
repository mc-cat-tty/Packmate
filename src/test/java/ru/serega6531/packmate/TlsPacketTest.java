package ru.serega6531.packmate;

import org.junit.jupiter.api.Test;
import org.pcap4j.packet.IllegalRawDataException;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.service.optimization.tls.TlsPacket;

import java.io.IOException;
import java.util.List;

public class TlsPacketTest {

    @Test
    public void testHandshake() throws IOException, IllegalRawDataException {
        List<Packet> packets = new PackmateDumpFileLoader("tls.pkmt").getPackets();
        byte[] content = packets.get(0).getContent();

        TlsPacket tlsPacket = TlsPacket.newPacket(content, 0, content.length);
        System.out.println(tlsPacket.toString());
    }

}

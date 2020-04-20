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
        List<Packet> packets = new PackmateDumpFileLoader("tls-wolfram.pkmt").getPackets();

        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);
            System.out.println("Packet " + i + ", incoming: " + packet.isIncoming());
            byte[] content = packet.getContent();
            TlsPacket tlsPacket = TlsPacket.newPacket(content, 0, content.length);
            System.out.println(tlsPacket.toString());
        }
    }

}

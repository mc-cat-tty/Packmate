package ru.serega6531.packmate;

import org.junit.jupiter.api.Test;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.service.optimization.TlsDecryptor;

import java.io.IOException;
import java.util.List;

public class TlsDecryptorTest {

    @Test
    public void testDecryptTls() throws IOException {
        List<Packet> packets = new PackmateDumpFileLoader("tls.pkmt").getPackets();

        TlsDecryptor decryptor = new TlsDecryptor(packets);
        decryptor.decryptTls();
    }

}

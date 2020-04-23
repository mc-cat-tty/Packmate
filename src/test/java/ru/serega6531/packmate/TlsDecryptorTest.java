package ru.serega6531.packmate;

import org.junit.jupiter.api.Test;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.service.optimization.RsaKeysHolder;
import ru.serega6531.packmate.service.optimization.TlsDecryptor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TlsDecryptorTest {

    @Test
    public void testDecryptTls() throws IOException {
        List<Packet> packets = new PackmateDumpFileLoader("tls.pkmt").getPackets();

        RsaKeysHolder keysHolder = new RsaKeysHolder();
        File pemFile = new File(getClass().getClassLoader().getResource("tls.pem").getFile());
        File keyFile = new File(getClass().getClassLoader().getResource("tls.key").getFile());
        keysHolder.addKey(pemFile, keyFile);

        TlsDecryptor decryptor = new TlsDecryptor(packets, keysHolder);
        decryptor.decryptTls();
    }

}

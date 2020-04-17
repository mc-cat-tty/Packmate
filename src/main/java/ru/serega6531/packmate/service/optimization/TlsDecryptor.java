package ru.serega6531.packmate.service.optimization;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.PacketUtils;
import ru.serega6531.packmate.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

@RequiredArgsConstructor
public class TlsDecryptor {

    private final List<Packet> packets;

    @SneakyThrows
    public void decryptTls() {
        List<List<Packet>> sides = PacketUtils.sliceToSides(packets);

        File pemFile = new File(getClass().getClassLoader().getResource("tls.pem").getFile());
        File keyFile = new File(getClass().getClassLoader().getResource("tls.key").getFile());
        SSLContext context = SSLUtils.createContext(pemFile, keyFile);
        SSLEngine serverEngine = context.createSSLEngine();
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(true);

        ByteBuffer decodedServerBuf = ByteBuffer.allocate(1000);

        SSLEngineResult unwrap = serverEngine.unwrap(ByteBuffer.wrap(packets.get(0).getContent()), decodedServerBuf);
        System.out.println();
    }

}

package ru.serega6531.packmate.service.optimization;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.crypto.codec.Hex;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.utils.PacketUtils;
import ru.serega6531.packmate.utils.SSLUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.File;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class TlsDecryptor {

    private final List<Packet> packets;

    @SneakyThrows
    public void decryptTls() {
        List<List<Packet>> sides = PacketUtils.sliceToSides(packets);

        File pemFile = new File(getClass().getClassLoader().getResource("tls.pem").getFile());
        File keyFile = new File(getClass().getClassLoader().getResource("tls.key").getFile());
        SSLContext context = SSLUtils.createContext(pemFile, keyFile, new TlsFakeSecureRandom());
        SSLEngine serverEngine = context.createSSLEngine();
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(true);

        ByteBuffer decodedServerBuf = ByteBuffer.allocate(1000);
        ByteBuffer tmp = ByteBuffer.allocate(50);
        ByteBuffer tmp2 = ByteBuffer.allocate(50000);
//        tmp.put((byte)1);

        unwrap(serverEngine, packets.get(0).getContent(), decodedServerBuf);
        wrap(serverEngine, tmp, tmp2);
        wrap(serverEngine, tmp, tmp2);
        wrap(serverEngine, tmp, tmp2);
        unwrap(serverEngine, packets.get(2).getContent(), decodedServerBuf);
        unwrap(serverEngine, packets.get(3).getContent(), decodedServerBuf);
        unwrap(serverEngine, packets.get(4).getContent(), decodedServerBuf);
        unwrap(serverEngine, packets.get(5).getContent(), decodedServerBuf);

        System.out.println();
    }

    @SneakyThrows
    private void unwrap(SSLEngine serverEngine, byte[] content, ByteBuffer buf) {
        SSLEngineResult unwrap = serverEngine.unwrap(ByteBuffer.wrap(content), buf);
        System.out.println("UNWRAP " + unwrap);
        Runnable delegatedTask = serverEngine.getDelegatedTask();
        if(delegatedTask != null) {
            delegatedTask.run();
        }
    }

    @SneakyThrows
    private void wrap(SSLEngine serverEngine, ByteBuffer src, ByteBuffer dest) {
        SSLEngineResult wrap = serverEngine.wrap(src, dest);
        System.out.println("WRAP " + wrap);
        Runnable delegatedTask = serverEngine.getDelegatedTask();
        if(delegatedTask != null) {
            delegatedTask.run();
        }
    }

    private static class TlsFakeSecureRandom extends SecureRandom {

        /*
        state 0 - engineInit(SSLContextImpl.java:117)
        stage 1 - SessionId.<init> -> RandomCookie
        stage 2 - server random (ServerHello.java:575)
        stage 3 - XDHKeyPairGenerator.generateKeyPair -> XECOperations.generatePrivate
         */

        private int state = 0;

        @Override
        public void nextBytes(byte[] bytes) {
            System.out.println("STATE " + state);
            StackWalker.getInstance().forEach(System.out::println);
            System.out.println("-----------------");

            switch (state) {
                case 0 -> Arrays.fill(bytes, (byte) 0);
                case 1, 2, 3 -> System.arraycopy(getFakeBytes(), 0, bytes, 0, bytes.length);
            }

            state++;
        }

        private byte[] getFakeBytes() {
            return switch (state) {
                case 1 -> Hex.decode("0ab8b3409555d3d658b1844f52dfc0116467c4b9088d1deb504f3935c10de893");
                case 2 -> Hex.decode("b5474b785c5e9bbadf2b0cd136e9aaf8bc2d89583ef96c479b531b94808349cc");
                case 3 -> Hex.decode("801d96be72cbbd2f4e33b5ec7e5e0b073636269e42c17d1d8996fdd28c9f7230");
                default -> throw new IllegalStateException("Unexpected value: " + state);
            };
        }

    }

}

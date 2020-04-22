package ru.serega6531.packmate.service.optimization;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.service.optimization.tls.TlsPacket;
import ru.serega6531.packmate.service.optimization.tls.keys.TlsKeyUtils;
import ru.serega6531.packmate.service.optimization.tls.numbers.CipherSuite;
import ru.serega6531.packmate.service.optimization.tls.numbers.ContentType;
import ru.serega6531.packmate.service.optimization.tls.numbers.HandshakeType;
import ru.serega6531.packmate.service.optimization.tls.records.HandshakeRecord;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.BasicRecordContent;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.ClientHelloHandshakeRecordContent;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.HandshakeRecordContent;
import ru.serega6531.packmate.service.optimization.tls.records.handshakes.ServerHelloHandshakeRecordContent;
import ru.serega6531.packmate.utils.PRF;
import ru.serega6531.packmate.utils.TlsUtils;

import javax.crypto.Cipher;
import javax.net.ssl.X509KeyManager;
import java.io.File;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TlsDecryptor {

    private static final Pattern cipherSuitePattern = Pattern.compile("TLS_RSA_WITH_([A-Z0-9_]+)_([A-Z0-9]+)");

    private final List<Packet> packets;

    @SneakyThrows
    public void decryptTls() {
        File pemFile = new File(getClass().getClassLoader().getResource("tls.pem").getFile());
        File keyFile = new File(getClass().getClassLoader().getResource("tls.key").getFile());
        X509KeyManager keyManager = TlsUtils.createKeyManager(pemFile, keyFile);

        X509Certificate[] certificateChain = keyManager.getCertificateChain("1");
        RSAPrivateKey privateKey = ((RSAPrivateKey) keyManager.getPrivateKey("1"));

        Map<Packet, List<TlsPacket.TlsHeader>> tlsPackets = packets.stream()
                .collect(Collectors.toMap(p -> p, this::createTlsHeaders));

        ClientHelloHandshakeRecordContent clientHello = (ClientHelloHandshakeRecordContent)
                        getHandshake(tlsPackets.values(), HandshakeType.CLIENT_HELLO).orElseThrow();
        ServerHelloHandshakeRecordContent serverHello = (ServerHelloHandshakeRecordContent)
                getHandshake(tlsPackets.values(), HandshakeType.SERVER_HELLO).orElseThrow();

        byte[] clientRandom = clientHello.getRandom();
        byte[] serverRandom = serverHello.getRandom();

        CipherSuite cipherSuite = serverHello.getCipherSuite();

        if(cipherSuite.name().startsWith("TLS_RSA_WITH_")) {
            Matcher matcher = cipherSuitePattern.matcher(cipherSuite.name());
            matcher.find();
            String blockCipher = matcher.group(1);
            String hashAlgo = matcher.group(2);

            BasicRecordContent clientKeyExchange = (BasicRecordContent)
                    getHandshake(tlsPackets.values(), HandshakeType.CLIENT_KEY_EXCHANGE).orElseThrow();

            byte[] encryptedPreMaster = TlsKeyUtils.getClientRsaPreMaster(clientKeyExchange.getContent(), 0);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] preMaster = cipher.doFinal(encryptedPreMaster);
            byte[] seed1 = ArrayUtils.addAll(clientRandom, serverRandom);
            byte[] seed2 = ArrayUtils.addAll(serverRandom, clientRandom);

            byte[] masterSecret = PRF.getBytes(preMaster, "master secret", seed1, 48);
            byte[] expanded = PRF.getBytes(masterSecret, "key expansion", seed2, 136);
            System.out.println();
        }

    }

    private Optional<HandshakeRecordContent> getHandshake(Collection<List<TlsPacket.TlsHeader>> packets,
                                                      HandshakeType handshakeType) {
        return packets.stream()
                .flatMap(Collection::stream)
                .filter(p -> p.getContentType() == ContentType.HANDSHAKE)
                .map(p -> ((HandshakeRecord) p.getRecord()))
                .filter(r -> r.getHandshakeType() == handshakeType)
                .map(HandshakeRecord::getContent)
                .findFirst();
    }

    @SneakyThrows
    private List<TlsPacket.TlsHeader> createTlsHeaders(Packet p) {
        List<TlsPacket.TlsHeader> headers = new ArrayList<>();
        TlsPacket tlsPacket = TlsPacket.newPacket(p.getContent(), 0, p.getContent().length);

        headers.add(tlsPacket.getHeader());

        while (tlsPacket.getPayload() != null) {
            tlsPacket = (TlsPacket) tlsPacket.getPayload();
            headers.add(tlsPacket.getHeader());
        }

        return headers;
    }

}

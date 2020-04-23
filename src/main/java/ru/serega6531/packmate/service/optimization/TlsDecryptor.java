package ru.serega6531.packmate.service.optimization;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.tls.ExporterLabel;
import org.bouncycastle.tls.PRFAlgorithm;
import org.bouncycastle.tls.crypto.TlsSecret;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsSecret;
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
import ru.serega6531.packmate.utils.TlsUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.X509KeyManager;
import java.io.File;
import java.nio.ByteBuffer;
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

        if (cipherSuite.name().startsWith("TLS_RSA_WITH_")) {
            Matcher matcher = cipherSuitePattern.matcher(cipherSuite.name());
            matcher.find();
            String blockCipher = matcher.group(1);
            String hashAlgo = matcher.group(2);

            BasicRecordContent clientKeyExchange = (BasicRecordContent)
                    getHandshake(tlsPackets.values(), HandshakeType.CLIENT_KEY_EXCHANGE).orElseThrow();

            byte[] encryptedPreMaster = TlsKeyUtils.getClientRsaPreMaster(clientKeyExchange.getContent(), 0);

            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] preMaster = rsa.doFinal(encryptedPreMaster);
            byte[] randomCS = ArrayUtils.addAll(clientRandom, serverRandom);
            byte[] randomSC = ArrayUtils.addAll(serverRandom, clientRandom);

            BcTlsSecret preSecret = new BcTlsSecret(new BcTlsCrypto(null), preMaster);
            TlsSecret masterSecret = preSecret.deriveUsingPRF(
                    PRFAlgorithm.tls_prf_sha256, ExporterLabel.master_secret, randomCS, 48);
            byte[] expanded = masterSecret.deriveUsingPRF(PRFAlgorithm.tls_prf_sha256, ExporterLabel.key_expansion, randomSC, 136).extract(); // для sha256

            byte[] clientMacKey = new byte[20];
            byte[] serverMacKey = new byte[20];
            byte[] clientEncryptionKey = new byte[32];
            byte[] serverEncryptionKey = new byte[32];
            byte[] clientIV = new byte[16];
            byte[] serverIV = new byte[16];

            ByteBuffer bb = ByteBuffer.wrap(expanded);
            bb.get(clientMacKey);
            bb.get(serverMacKey);
            bb.get(clientEncryptionKey);
            bb.get(serverEncryptionKey);
            bb.get(clientIV);
            bb.get(serverIV);

            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");  // TLS_RSA_WITH_AES_256_CBC_SHA
            SecretKeySpec skeySpec = new SecretKeySpec(clientEncryptionKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(clientIV);
            aes.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);

            byte[] data = tlsPackets.entrySet().stream()
                    .filter(ent -> ent.getKey().isIncoming())
                    .map(Map.Entry::getValue)
                    .flatMap(Collection::stream)
                    .filter(p -> p.getContentType() == ContentType.HANDSHAKE)
                    .map(p -> ((HandshakeRecord) p.getRecord()))
                    .filter(r -> r.getHandshakeType() == HandshakeType.ENCRYPTED_HANDSHAKE_MESSAGE)
                    .map(r -> ((BasicRecordContent) r.getContent()))
                    .findFirst()
                    .orElseThrow()
                    .getContent();

            byte[] decrypt = aes.doFinal(data);
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

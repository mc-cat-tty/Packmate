package ru.serega6531.packmate.service.optimization.tls.keys;

import ru.serega6531.packmate.service.optimization.tls.keys.enums.CurveType;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.NamedCurve;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.SignatureHashAlgorithmHash;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.SignatureHashAlgorithmSignature;
import ru.serega6531.packmate.service.optimization.tls.numbers.TlsVersion;

import java.nio.ByteBuffer;

/**
 * It is impossible to determine key format just by KeyExchange record,
 * so you can use this class when analyzing tls traffic.
 */
public final class TlsKeyUtils {

    // https://wiki.osdev.org/TLS_Handshake

    public static DhClientParams parseServerDH(byte[] rawData, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(rawData).position(offset);

        short pLength = bb.getShort();
        byte[] p = new byte[pLength];
        bb.get(p);

        short gLength = bb.getShort();
        byte[] g = new byte[gLength];
        bb.get(g);

        short pubKeyLength = bb.getShort();
        byte[] pubKey = new byte[pubKeyLength];  // aka Ys
        bb.get(pubKey);

        SignatureHashAlgorithmHash signatureHashAlgorithmHash =
                SignatureHashAlgorithmHash.findByValue(bb.getShort());
        SignatureHashAlgorithmSignature signatureHashAlgorithmSignature =
                SignatureHashAlgorithmSignature.findByValue(bb.getShort());

        if (signatureHashAlgorithmHash == null || signatureHashAlgorithmSignature == null) {
            throw new IllegalArgumentException("Unknown signature data");
        }

        short signatureLength = bb.getShort();
        byte[] signature = new byte[signatureLength];

        bb.get(signature);

        return new DhClientParams(p, g, pubKey, signatureHashAlgorithmHash, signatureHashAlgorithmSignature, signature);
    }

    /**
     * @param rawData Handshake record content
     */
    public static EcdheServerParams parseServerECDHE(byte[] rawData, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(rawData).position(offset);

        byte curveTypeId = bb.get();
        if(curveTypeId != 0x03) {
            throw new IllegalArgumentException("Unsupported curve type");
        }

        CurveType curveType = CurveType.NAMED;
        NamedCurve namedCurve = NamedCurve.findByValue(bb.getShort());

        if (namedCurve == null) {
            throw new IllegalArgumentException("Unsupported named curve");
        }

        byte pubkeyLength = bb.get();
        byte[] pubkey = new byte[pubkeyLength];
        bb.get(pubkey);

        SignatureHashAlgorithmHash signatureHashAlgorithmHash =
                SignatureHashAlgorithmHash.findByValue(bb.getShort());
        SignatureHashAlgorithmSignature signatureHashAlgorithmSignature =
                SignatureHashAlgorithmSignature.findByValue(bb.getShort());

        if (signatureHashAlgorithmHash == null || signatureHashAlgorithmSignature == null) {
            throw new IllegalArgumentException("Unknown signature data");
        }

        short signatureLength = bb.getShort();
        byte[] signature = new byte[signatureLength];

        bb.get(signature);

        return new EcdheServerParams(curveType, namedCurve, pubkey,
                signatureHashAlgorithmHash, signatureHashAlgorithmSignature, signature);
    }

    // https://ldapwiki.com/wiki/ClientKeyExchange

    /**
     * Suitable for both DH and ECDHE
     * @param rawData Handshake record content
     */
    public static byte[] getClientDHPubkey(byte[] rawData, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(rawData).position(offset);

        byte length = bb.get();
        byte[] pubkey = new byte[length];
        bb.get(pubkey);

        return pubkey;
    }

    public static RsaServerParams parseClientRsa(byte[] rawData, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(rawData).position(offset);

        TlsVersion version = TlsVersion.getInstance(bb.getShort());
        byte[] encryptedPreMasterSecret = new byte[46];
        bb.get(encryptedPreMasterSecret);

        return new RsaServerParams(version, encryptedPreMasterSecret);
    }

}

package ru.serega6531.packmate.service.optimization.tls.keys;

import ru.serega6531.packmate.service.optimization.tls.keys.enums.CurveType;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.NamedCurve;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.SignatureHashAlgorithmHash;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.SignatureHashAlgorithmSignature;

import java.nio.ByteBuffer;

public final class TlsKeyUtils {

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

    /**
     * @param rawData Handshake record content
     */
    public static byte[] getServerECDHEPubkey(byte[] rawData, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(rawData).position(offset);

        byte length = bb.get();
        byte[] pubkey = new byte[length];
        bb.get(pubkey);

        return pubkey;
    }

}

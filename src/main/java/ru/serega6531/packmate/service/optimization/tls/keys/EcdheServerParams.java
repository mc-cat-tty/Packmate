package ru.serega6531.packmate.service.optimization.tls.keys;

import ru.serega6531.packmate.service.optimization.tls.keys.enums.CurveType;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.NamedCurve;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.SignatureHashAlgorithmHash;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.SignatureHashAlgorithmSignature;

public class EcdheServerParams {

    private CurveType curveType;
    private NamedCurve namedCurve;
    private byte[] pubkey;
    private SignatureHashAlgorithmHash signatureHashAlgorithmHash;
    private SignatureHashAlgorithmSignature signatureHashAlgorithmSignature;
    private byte[] signature;

    public EcdheServerParams(CurveType curveType, NamedCurve namedCurve, byte[] pubkey,
                             SignatureHashAlgorithmHash signatureHashAlgorithmHash,
                             SignatureHashAlgorithmSignature signatureHashAlgorithmSignature,
                             byte[] signature) {
        this.curveType = curveType;
        this.namedCurve = namedCurve;
        this.pubkey = pubkey;
        this.signatureHashAlgorithmHash = signatureHashAlgorithmHash;
        this.signatureHashAlgorithmSignature = signatureHashAlgorithmSignature;
        this.signature = signature;
    }

    public CurveType getCurveType() {
        return curveType;
    }

    public NamedCurve getNamedCurve() {
        return namedCurve;
    }

    public byte[] getPubkey() {
        return pubkey;
    }

    public SignatureHashAlgorithmHash getSignatureHashAlgorithmHash() {
        return signatureHashAlgorithmHash;
    }

    public SignatureHashAlgorithmSignature getSignatureHashAlgorithmSignature() {
        return signatureHashAlgorithmSignature;
    }

    public byte[] getSignature() {
        return signature;
    }
}

package ru.serega6531.packmate.service.optimization.tls.keys;

import ru.serega6531.packmate.service.optimization.tls.keys.enums.SignatureHashAlgorithmHash;
import ru.serega6531.packmate.service.optimization.tls.keys.enums.SignatureHashAlgorithmSignature;

public class DhClientParams {

    private final byte[] p;
    private final byte[] g;
    private final byte[] pubkey;
    private final SignatureHashAlgorithmHash signatureHashAlgorithmHash;
    private final SignatureHashAlgorithmSignature signatureHashAlgorithmSignature;
    private final byte[] signature;

    public DhClientParams(byte[] p, byte[] g, byte[] pubkey,
                          SignatureHashAlgorithmHash signatureHashAlgorithmHash,
                          SignatureHashAlgorithmSignature signatureHashAlgorithmSignature,
                          byte[] signature) {
        this.p = p;
        this.g = g;
        this.pubkey = pubkey;
        this.signatureHashAlgorithmHash = signatureHashAlgorithmHash;
        this.signatureHashAlgorithmSignature = signatureHashAlgorithmSignature;
        this.signature = signature;
    }

    public byte[] getP() {
        return p;
    }

    public byte[] getG() {
        return g;
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

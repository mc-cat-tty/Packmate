package ru.serega6531.packmate.service.optimization.tls.keys;

import ru.serega6531.packmate.service.optimization.tls.numbers.TlsVersion;

public class RsaServerParams {

    private final TlsVersion version;
    private final byte[] encryptedPreMasterSecret;

    public RsaServerParams(TlsVersion version, byte[] encryptedPreMasterSecret) {
        this.version = version;
        this.encryptedPreMasterSecret = encryptedPreMasterSecret;
    }

    public TlsVersion getVersion() {
        return version;
    }

    public byte[] getEncryptedPreMasterSecret() {
        return encryptedPreMasterSecret;
    }
}

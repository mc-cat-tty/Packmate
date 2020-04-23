package ru.serega6531.packmate.service.optimization;

import org.springframework.stereotype.Service;
import ru.serega6531.packmate.utils.TlsUtils;

import javax.net.ssl.X509KeyManager;
import java.io.File;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

@Service
public class RsaKeysHolder {

    // Key: N from RSA public key
    private final Map<BigInteger, RSAPrivateKey> keys = new HashMap<>();

    public void addKey(File pemFile, File keyFile) {
        if(!pemFile.exists() || !keyFile.exists()) {
            throw new IllegalArgumentException("One of files does not exist");
        }

        X509KeyManager keyManager = TlsUtils.createKeyManager(pemFile, keyFile);

//        X509Certificate[] certificateChain = keyManager.getCertificateChain("1");
        RSAPrivateKey privateKey = ((RSAPrivateKey) keyManager.getPrivateKey("1"));
        keys.put(privateKey.getModulus(), privateKey);
    }

    public RSAPrivateKey getKey(BigInteger modulus) {
        return keys.get(modulus);
    }

}

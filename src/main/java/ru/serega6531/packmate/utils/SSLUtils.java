package ru.serega6531.packmate.utils;

import com.google.common.base.Splitter;
import lombok.SneakyThrows;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import static com.google.common.base.Preconditions.checkState;

public class SSLUtils {

    @SneakyThrows
    public static SSLContext createContext(File pemFile, File keyFile, SecureRandom random) {
        final String pass = "abcdef";

        File jksKeystoreFile = File.createTempFile("packmate_", ".jks");
        File pkcsKeystoreFile = File.createTempFile("packmate_", ".pkcs12");
        Splitter splitter = Splitter.on(' ');

        jksKeystoreFile.delete();

        String command = "openssl pkcs12 -export -out " + pkcsKeystoreFile.getAbsolutePath() + " -in " + pemFile.getAbsolutePath() +
                " -inkey " + keyFile.getAbsolutePath() + " -passout pass:" + pass;

        Process process = new ProcessBuilder(splitter.splitToList(command)).inheritIO().start();
        checkState(process.waitFor() == 0);

        command = "keytool -importkeystore -srckeystore " + pkcsKeystoreFile.getAbsolutePath() + " -srcstoretype PKCS12 -destkeystore " +
                jksKeystoreFile.getAbsolutePath() + " -srcstorepass " + pass + " -deststorepass " + pass;

        process = new ProcessBuilder(splitter.splitToList(command)).inheritIO().start();
        checkState(process.waitFor() == 0);

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(new FileInputStream(jksKeystoreFile), pass.toCharArray());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, pass.toCharArray());

        SSLContext ret = SSLContext.getInstance("TLSv1.2");
        TrustManagerFactory factory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keystore);
        ret.init(keyManagerFactory.getKeyManagers(), factory.getTrustManagers(), random);

        return ret;
    }

}

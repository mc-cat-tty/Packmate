package ru.serega6531.packmate.utils;

import lombok.experimental.UtilityClass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Based on <a href="https://github.com/NivekT/Java/blob/master/SecureChannel/PRF.java">PRF.java</a>
 */
@UtilityClass
public class PRF {

    private static final MessageDigest md5;
    private static final MessageDigest sha;
    private static final HMAC hmac = new HMAC(null, null);

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
            sha = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 or SHA failed to initialize");
        }
    }

    /**
     * Generates the PRF of the given inputs
     * @param secret
     * @param label
     * @param seed
     * @param length    The length of the output to generate.
     * @return	PRF of inputs
     */
    public byte[] getBytes(byte[] secret, String label, byte[] seed, int length) {

        byte[] output = new byte[length];

        // split secret into S1 and S2
        int lenS1 = secret.length / 2 + secret.length % 2;

        byte[] s1 = new byte[lenS1];
        byte[] s2 = new byte[lenS1];

        System.arraycopy(secret, 0, s1, 0, lenS1);
        System.arraycopy(secret, secret.length - lenS1, s2, 0, lenS1);

        // get the seed as concatenation of label and seed
        byte[] labelAndSeed = new byte[label.length() + seed.length];
        System.arraycopy(label.getBytes(), 0, labelAndSeed, 0, label.length());
        System.arraycopy(seed, 0, labelAndSeed, label.length(), seed.length);

        byte[] md5Output = p_hash(md5, 16, s1, labelAndSeed, length);
        byte[] shaOutput = p_hash(sha, 20, s2, labelAndSeed, length);

        // XOR md5 and sha to get output
        for (int i = 0; i < length; i++) {
            output[i] = (byte) (md5Output[i] ^ shaOutput[i]);
        }

        return output;
    }

    /**
     * Perform the P_hash function
     * @param md     The MessageDigest function to use
     * @param digestLength The length of output from the given digest
     * @param secret   The TLS secret
     * @param seed     The seed to use
     * @param length The desired length of the output.
     * @return The P_hash of the inputs.
     */
    private byte[] p_hash(MessageDigest md, int digestLength, byte[] secret,
                          byte[] seed, int length) {

        // set up our hmac
        hmac.setMD(md);
        hmac.setKey(secret);

        byte[] output = new byte[length];   // what we return
        int offset = 0;     // how much data we have created so far
        int toCopy = 0;     // the amount of data to copy from current HMAC

        byte[] a = seed;    // initialise A(0)

        // concatenation of A and seed
        byte[] aSeed = new byte[digestLength + seed.length];
        System.arraycopy(seed, 0, aSeed, digestLength, seed.length);

        byte[] tempBuf = null;

        // continually perform HMACs and concatenate until we have enough output
        while( offset < length ) {

            // calculate the A to use.
            a = hmac.digest(a);

            // concatenate A and seed and perform HMAC
            System.arraycopy(a, 0, aSeed, 0, digestLength);
            tempBuf = hmac.digest(aSeed);

            // work out how much needs to be copied and copy it
            toCopy = Math.min(tempBuf.length, (length - offset));
            System.arraycopy(tempBuf, 0, output, offset, toCopy);
            offset += toCopy;
        }
        return output;
    }

}

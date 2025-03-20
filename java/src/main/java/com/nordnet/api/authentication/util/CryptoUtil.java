package com.nordnet.api.authentication.util;

import org.bouncycastle.jcajce.spec.OpenSSHPrivateKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.util.Base64;

public class CryptoUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String signString(String data, String privateKey) throws Exception {
        byte[] encoded = Base64.getDecoder().decode(privateKey
                .replace("-----BEGIN OPENSSH PRIVATE KEY-----", "")
                .replace("-----END OPENSSH PRIVATE KEY-----", "")
                .replaceAll("\\s+", ""));
        OpenSSHPrivateKeySpec keySpec = new OpenSSHPrivateKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");

        PrivateKey privKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance("Ed25519", "BC");
        signature.initSign(privKey);
        signature.update(data.getBytes());

        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
}

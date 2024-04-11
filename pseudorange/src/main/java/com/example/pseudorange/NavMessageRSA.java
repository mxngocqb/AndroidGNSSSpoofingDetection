package com.example.pseudorange;

import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Scanner;;

public class NavMessageRSA {
    private PublicKey publicKey;

    public NavMessageRSA() {
        try {
            byte[] modulusBytes = Base64.getDecoder().decode("2h/vNGgtsnzM9si8Zr5e4Wy6X1v3WoiCnnKBUpK3nf5OIVjtq5raJVx/CY/u0HqZpPr8WodcNENBtHKOt9LH/SAtqS0UxNhzurMiDNxZCzsuoT9dGmBche7nUmxkclZO+0aBb+kw7TJl2pC0USr2njl9tbLdTjwxrxFQK/sidmU=");
            byte[] exponentBytes = Base64.getDecoder().decode("AQAB");
            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = keyFactory.generatePublic(rsaPublicKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean verifySignature(String data, String signature) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            boolean verified = sig.verify(signatureBytes);
            return verified;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

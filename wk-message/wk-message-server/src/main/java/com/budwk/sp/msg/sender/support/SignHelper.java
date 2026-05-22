package com.budwk.sp.msg.sender.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class SignHelper {
    private SignHelper() {
    }

    public static String hmacSha1Base64(String secret, String data) {
        return hmacBase64("HmacSHA1", secret, data);
    }

    public static String hmacSha256Base64(String secret, String data) {
        return hmacBase64("HmacSHA256", secret, data);
    }

    public static byte[] hmacSha256Bytes(byte[] secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("sign error", e);
        }
    }

    public static String percentEncode(String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    private static String hmacBase64(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] signData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            throw new IllegalStateException("sign error", e);
        }
    }
}

package com.budwk.sp.starter.common.utils;

import org.nutz.lang.Lang;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PwdUtil {
    private static final SecureRandom random = new SecureRandom();

    public static String getPassword(String passowrd, String salt) {
        byte[] bytes = hash(passowrd.getBytes(), salt.getBytes(), 1024);
        if (bytes != null) {
            return Lang.fixedHexString(bytes);
        }
        return "";
    }

    // 定义字符池：包含大写、小写、数字以及你要求的特殊字符
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "0123456789" +
            "@#";

    public static String generateRandomString(int length) {
        return IntStream.range(0, length)
                .map(i -> CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())))
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
    }

    public static byte[] hash(byte[] bytes, byte[] salt, int hashIterations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (salt != null) {
                digest.reset();
                digest.update(salt);
            }

            byte[] hashed = digest.digest(bytes);
            int iterations = hashIterations - 1;

            for (int i = 0; i < iterations; ++i) {
                digest.reset();
                hashed = digest.digest(hashed);
            }
            return hashed;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

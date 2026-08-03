package com.fresh.common.util;

import cn.hutool.crypto.digest.DigestUtil;

public final class CryptoUtils {

    private static final String SALT = "fresh-group-buy";

    private CryptoUtils() {
    }

    public static String encryptPassword(String rawPassword) {
        return DigestUtil.md5Hex(rawPassword + SALT);
    }

    public static boolean matchPassword(String rawPassword, String encrypted) {
        return encryptPassword(rawPassword).equals(encrypted);
    }
}

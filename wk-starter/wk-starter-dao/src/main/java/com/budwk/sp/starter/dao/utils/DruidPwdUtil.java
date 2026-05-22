package com.budwk.sp.starter.dao.utils;

public class DruidPwdUtil {
    public static void main(String[] args) throws Exception {
        String password = "postgresql";
        String[] keyPair = com.alibaba.druid.filter.config.ConfigTools.genKeyPair(512);
        // keyPair[0] 是私钥，keyPair[1] 是公钥
        String encryptPassword = com.alibaba.druid.filter.config.ConfigTools.encrypt(keyPair[0], password);

        System.out.println("公钥: " + keyPair[1]);
        System.out.println("加密后的密码: " + encryptPassword);
    }
}

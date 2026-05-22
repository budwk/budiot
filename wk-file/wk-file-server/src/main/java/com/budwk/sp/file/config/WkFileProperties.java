package com.budwk.sp.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wk.file")
public class WkFileProperties {
    private String defaultStorage = "LOCAL";
    private long maxUploadSize = 512000;
    private Local local = new Local();
    private Ftp ftp = new Ftp();
    private Fdfs fdfs = new Fdfs();
    private Minio minio = new Minio();

    @Data
    public static class Local {
        private String baseDir = System.getProperty("user.home") + "/budiot/upload";
    }

    @Data
    public static class Ftp {
        private String host;
        private int port = 21;
        private String username;
        private String password;
        private String baseDir = "/budiot/upload";
        private boolean passiveMode = true;
        private int connectTimeout = 10000;
        private int dataTimeout = 15000;
        private String encoding = "UTF-8";
    }

    @Data
    public static class Fdfs {
        private String trackerServers;
        private int connectTimeout = 5000;
        private int networkTimeout = 30000;
        private String charset = "UTF-8";
    }

    @Data
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket = "budiot";
        private String region;
        private String baseDir = "upload";
    }
}

package com.budwk.sp.file.storage.impl;

import com.budwk.sp.file.config.WkFileProperties;
import com.budwk.sp.file.enums.FileStorageType;
import com.budwk.sp.file.storage.FileStorageProvider;
import com.budwk.sp.file.storage.StoredFile;
import com.budwk.sp.file.util.FileStoragePathHelper;
import com.budwk.sp.starter.common.exception.BaseException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
@Component
public class FtpFileStorageProvider implements FileStorageProvider {
    private final WkFileProperties wkFileProperties;

    public FtpFileStorageProvider(WkFileProperties wkFileProperties) {
        this.wkFileProperties = wkFileProperties;
    }

    @Override
    public boolean supports(FileStorageType storageType) {
        return storageType == FileStorageType.FTP;
    }

    @Override
    public StoredFile store(String tenantId, MultipartFile file, String suffix, String subPath) throws IOException {
        String storageKey = buildStorageKey(tenantId, suffix, subPath);
        FTPClient client = login();
        try (var inputStream = file.getInputStream()) {
            ensureDirectory(client, parent(storageKey));
            if (!client.storeFile(storageKey, inputStream)) {
                throw new BaseException("FTP 文件上传失败");
            }
            return new StoredFile(storageKey);
        } finally {
            logout(client);
        }
    }

    @Override
    public void writeTo(String storageKey, OutputStream outputStream) throws IOException {
        FTPClient client = login();
        try {
            if (!client.retrieveFile(storageKey, outputStream)) {
                throw new BaseException("FTP 文件读取失败");
            }
        } finally {
            logout(client);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        FTPClient client = login();
        try {
            client.deleteFile(storageKey);
        } finally {
            logout(client);
        }
    }

    private FTPClient login() throws IOException {
        WkFileProperties.Ftp ftp = wkFileProperties.getFtp();
        if (ftp.getHost() == null || ftp.getHost().isBlank()) {
            throw new BaseException("FTP 存储未配置 host");
        }
        FTPClient client = new FTPClient();
        client.setConnectTimeout(ftp.getConnectTimeout());
        client.setDataTimeout(ftp.getDataTimeout());
        client.connect(ftp.getHost(), ftp.getPort());
        if (!client.login(ftp.getUsername(), ftp.getPassword())) {
            throw new BaseException("FTP 登录失败");
        }
        client.setControlEncoding(ftp.getEncoding());
        client.setFileType(FTP.BINARY_FILE_TYPE);
        if (ftp.isPassiveMode()) {
            client.enterLocalPassiveMode();
        }
        return client;
    }

    private void logout(FTPClient client) throws IOException {
        if (client == null) {
            return;
        }
        if (client.isConnected()) {
            client.logout();
            client.disconnect();
        }
    }

    private void ensureDirectory(FTPClient client, String directory) throws IOException {
        if (directory == null || directory.isBlank()) {
            return;
        }
        String[] segments = directory.split("/");
        String current = "";
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            current += "/" + segment;
            client.makeDirectory(current);
        }
    }

    private String buildStorageKey(String tenantId, String suffix, String subPath) {
        WkFileProperties.Ftp ftp = wkFileProperties.getFtp();
        return (ftp.getBaseDir() + "/" + FileStoragePathHelper.buildStoragePath(tenantId, suffix, subPath))
                .replace("//", "/");
    }

    private String parent(String path) {
        int index = path.lastIndexOf('/');
        return index > 0 ? path.substring(0, index) : "";
    }
}

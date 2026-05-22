package com.budwk.sp.file.storage.impl;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.domain.proto.storage.DownloadCallback;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.budwk.sp.file.config.WkFileProperties;
import com.budwk.sp.file.enums.FileStorageType;
import com.budwk.sp.file.storage.FileStorageProvider;
import com.budwk.sp.file.storage.StoredFile;
import com.budwk.sp.starter.common.exception.BaseException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class FastDfsFileStorageProvider implements FileStorageProvider {
    private final ObjectProvider<FastFileStorageClient> fastFileStorageClientProvider;

    public FastDfsFileStorageProvider(ObjectProvider<FastFileStorageClient> fastFileStorageClientProvider) {
        this.fastFileStorageClientProvider = fastFileStorageClientProvider;
    }

    @Override
    public boolean supports(FileStorageType storageType) {
        return storageType == FileStorageType.FDFS;
    }

    @Override
    public StoredFile store(String tenantId, MultipartFile file, String suffix, String subPath) throws IOException {
        try {
            StorePath storePath = client().uploadFile(file.getInputStream(), file.getSize(), suffix, null);
            return new StoredFile(storePath.getFullPath());
        } catch (Exception e) {
            throw new IOException("FastDFS 文件上传失败", e);
        }
    }

    @Override
    public void writeTo(String storageKey, OutputStream outputStream) throws IOException {
        try {
            StorePath storePath = StorePath.parseFromUrl(storageKey);
            client().downloadFile(storePath.getGroup(), storePath.getPath(), (DownloadCallback<Void>) inputStream -> {
                inputStream.transferTo(outputStream);
                return null;
            });
        } catch (Exception e) {
            throw new IOException("FastDFS 文件读取失败", e);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            client().deleteFile(storageKey);
        } catch (Exception e) {
            throw new IOException("FastDFS 文件删除失败", e);
        }
    }

    private FastFileStorageClient client() {
        FastFileStorageClient client = fastFileStorageClientProvider.getIfAvailable();
        if (client == null) {
            throw new BaseException("FastDFS 客户端未启用，请检查存储配置");
        }
        return client;
    }
}

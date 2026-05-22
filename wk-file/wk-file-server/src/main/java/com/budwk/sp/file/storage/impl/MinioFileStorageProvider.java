package com.budwk.sp.file.storage.impl;

import com.budwk.sp.file.config.WkFileProperties;
import com.budwk.sp.file.enums.FileStorageType;
import com.budwk.sp.file.storage.FileStorageProvider;
import com.budwk.sp.file.storage.StoredFile;
import com.budwk.sp.file.util.FileStoragePathHelper;
import com.budwk.sp.starter.common.exception.BaseException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
@Component
public class MinioFileStorageProvider implements FileStorageProvider {
    private final WkFileProperties wkFileProperties;
    private volatile MinioClient minioClient;

    public MinioFileStorageProvider(WkFileProperties wkFileProperties) {
        this.wkFileProperties = wkFileProperties;
    }

    @Override
    public boolean supports(FileStorageType storageType) {
        return storageType == FileStorageType.MINIO;
    }

    @Override
    public StoredFile store(String tenantId, MultipartFile file, String suffix, String subPath) throws IOException {
        String objectKey = buildObjectKey(tenantId, suffix, subPath);
        try {
            ensureBucket();
            minio().putObject(PutObjectArgs.builder()
                    .bucket(wkFileProperties.getMinio().getBucket())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return new StoredFile(objectKey);
        } catch (Exception e) {
            throw new IOException("MinIO 文件上传失败", e);
        }
    }

    @Override
    public void writeTo(String storageKey, OutputStream outputStream) throws IOException {
        try (var inputStream = minio().getObject(GetObjectArgs.builder()
                .bucket(wkFileProperties.getMinio().getBucket())
                .object(storageKey)
                .build())) {
            inputStream.transferTo(outputStream);
        } catch (Exception e) {
            throw new IOException("MinIO 文件读取失败", e);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            minio().removeObject(RemoveObjectArgs.builder()
                    .bucket(wkFileProperties.getMinio().getBucket())
                    .object(storageKey)
                    .build());
        } catch (Exception e) {
            throw new IOException("MinIO 文件删除失败", e);
        }
    }

    private void ensureBucket() throws Exception {
        if (!minio().bucketExists(BucketExistsArgs.builder()
                .bucket(wkFileProperties.getMinio().getBucket())
                .build())) {
            minio().makeBucket(MakeBucketArgs.builder()
                    .bucket(wkFileProperties.getMinio().getBucket())
                    .build());
        }
    }

    private MinioClient minio() {
        if (minioClient == null) {
            synchronized (this) {
                if (minioClient == null) {
                    WkFileProperties.Minio minio = wkFileProperties.getMinio();
                    if (minio.getEndpoint() == null || minio.getEndpoint().isBlank()) {
                        throw new BaseException("MinIO 存储未配置 endpoint");
                    }
                    MinioClient.Builder builder = MinioClient.builder()
                            .endpoint(minio.getEndpoint())
                            .credentials(minio.getAccessKey(), minio.getSecretKey());
                    if (minio.getRegion() != null && !minio.getRegion().isBlank()) {
                        builder.region(minio.getRegion());
                    }
                    minioClient = builder.build();
                }
            }
        }
        return minioClient;
    }

    private String buildObjectKey(String tenantId, String suffix, String subPath) {
        String baseDir = wkFileProperties.getMinio().getBaseDir();
        String prefix = baseDir == null || baseDir.isBlank() ? "" : baseDir + "/";
        return prefix + FileStoragePathHelper.buildStoragePath(tenantId, suffix, subPath);
    }
}

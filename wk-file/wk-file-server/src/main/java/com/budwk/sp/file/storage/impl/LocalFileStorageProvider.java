package com.budwk.sp.file.storage.impl;

import com.budwk.sp.file.config.WkFileProperties;
import com.budwk.sp.file.enums.FileStorageType;
import com.budwk.sp.file.storage.FileStorageProvider;
import com.budwk.sp.file.storage.StoredFile;
import com.budwk.sp.file.util.FileStoragePathHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
@Component
public class LocalFileStorageProvider implements FileStorageProvider {
    private final WkFileProperties wkFileProperties;

    public LocalFileStorageProvider(WkFileProperties wkFileProperties) {
        this.wkFileProperties = wkFileProperties;
    }

    @Override
    public boolean supports(FileStorageType storageType) {
        return storageType == FileStorageType.LOCAL;
    }

    @Override
    public StoredFile store(String tenantId, MultipartFile file, String suffix, String subPath) throws IOException {
        String relativePath = FileStoragePathHelper.buildStoragePath(tenantId, suffix, subPath);
        Path target = Paths.get(wkFileProperties.getLocal().getBaseDir(), relativePath);
        Files.createDirectories(target.getParent());
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return new StoredFile(relativePath.replace('\\', '/'));
    }

    @Override
    public void writeTo(String storageKey, OutputStream outputStream) throws IOException {
        Files.copy(Paths.get(wkFileProperties.getLocal().getBaseDir(), storageKey), outputStream);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(Paths.get(wkFileProperties.getLocal().getBaseDir(), storageKey));
    }

}

package com.budwk.sp.file.storage;

import com.budwk.sp.file.enums.FileStorageType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;

public interface FileStorageProvider {
    boolean supports(FileStorageType storageType);

    StoredFile store(String tenantId, MultipartFile file, String suffix, String subPath) throws IOException;

    void writeTo(String storageKey, OutputStream outputStream) throws IOException;

    void delete(String storageKey) throws IOException;
}

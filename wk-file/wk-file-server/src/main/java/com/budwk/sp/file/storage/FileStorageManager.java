package com.budwk.sp.file.storage;

import com.budwk.sp.file.config.WkFileProperties;
import com.budwk.sp.file.enums.FileStorageType;
import com.budwk.sp.starter.common.exception.BaseException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileStorageManager {
    private final WkFileProperties wkFileProperties;
    private final List<FileStorageProvider> providers;

    public FileStorageManager(WkFileProperties wkFileProperties, List<FileStorageProvider> providers) {
        this.wkFileProperties = wkFileProperties;
        this.providers = providers;
    }

    public FileStorageType getDefaultStorageType() {
        return FileStorageType.fromValue(wkFileProperties.getDefaultStorage());
    }

    public FileStorageProvider getProvider(FileStorageType storageType) {
        return providers.stream()
                .filter(provider -> provider.supports(storageType))
                .findFirst()
                .orElseThrow(() -> new BaseException("未找到文件存储实现: " + storageType.getValue()));
    }
}

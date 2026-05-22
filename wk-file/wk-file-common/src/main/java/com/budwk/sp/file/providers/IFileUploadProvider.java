package com.budwk.sp.file.providers;

import com.budwk.sp.file.dto.FileUploadRequestDTO;
import com.budwk.sp.file.dto.FileUploadResultDTO;

public interface IFileUploadProvider {
    FileUploadResultDTO upload(FileUploadRequestDTO request);

    void delete(String id, String tenantId);
}

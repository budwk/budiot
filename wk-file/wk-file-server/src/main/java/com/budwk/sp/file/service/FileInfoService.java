package com.budwk.sp.file.service;

import com.budwk.sp.file.dto.FileInfoQueryDTO;
import com.budwk.sp.file.dto.FileUploadResultDTO;
import com.budwk.sp.file.entity.File_info;
import com.budwk.sp.starter.common.page.Pagination;
import com.budwk.sp.starter.database.service.BaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileInfoService extends BaseService<File_info> {
    List<FileUploadResultDTO> upload(MultipartFile[] files, String category, String subPath, boolean publicFlag,
                                     String operatorId, String operatorLoginname, String operatorUsername, String tenantId);

    Pagination list(FileInfoQueryDTO dto, String tenantId);

    File_info getFileInfo(String id, String tenantId);

    void preview(String id, String tenantId, HttpServletResponse response, boolean requirePublic);

    void download(String id, String tenantId, HttpServletResponse response);

    void updatePublicFlag(String id, boolean publicFlag, String operatorId, String tenantId);

    void deleteFile(String id, String tenantId);
}

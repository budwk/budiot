package com.budwk.sp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "文件上传请求DTO")
public class FileUploadRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String contentType;
    private byte[] bytes;
    private String category;
    private String subPath;
    private boolean publicFlag = true;
    private String operatorId;
    private String operatorLoginname;
    private String operatorUsername;
    private String tenantId;
}

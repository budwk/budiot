package com.budwk.sp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "文件上传结果DTO")
public class FileUploadResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String filename;
    private String url;
    private String contentType;
    private long size;
    private boolean image;
}

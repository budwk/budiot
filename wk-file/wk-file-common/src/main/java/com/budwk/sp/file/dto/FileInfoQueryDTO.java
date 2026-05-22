package com.budwk.sp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "文件分页查询DTO")
public class FileInfoQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String category;
    private String storageType;
    private Long beginTime;
    private Long endTime;
    private int pageNo = 1;
    private int pageSize = 10;
    private String pageOrderName = "createdAt";
    private String pageOrderBy = "descending";
}

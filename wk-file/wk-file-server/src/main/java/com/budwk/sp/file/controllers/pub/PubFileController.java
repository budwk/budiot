package com.budwk.sp.file.controllers.pub;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.file.dto.FileUploadResultDTO;
import com.budwk.sp.file.service.FileInfoService;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.nutz.lang.Strings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/pub/file")
@Tag(name = "文件公共接口", description = "文件上传与公开预览接口")
public class PubFileController {
    private final FileInfoService fileInfoService;

    public PubFileController(FileInfoService fileInfoService) {
        this.fileInfoService = fileInfoService;
    }

    @PostMapping("/upload/{type}")
    @SaCheckLogin
    @Operation(summary = "上传公共文件")
    public Result<FileUploadResultDTO> upload(
            @Parameter(description = "上传类型 image/file/video/blob") @PathVariable String type,
            @Parameter(description = "上传文件") @RequestParam("Filedata") MultipartFile file,
            @Parameter(description = "租户内子目录") @RequestParam(required = false) String subPath) {
        String operatorId = StpUtil.getLoginIdAsString();
        String tenantId = Strings.sBlank(StpUtil.getSession().getString("tenantId"), GlobalConstant.TENANT_ID_DEFAULT);
        FileUploadResultDTO result = fileInfoService.upload(new MultipartFile[]{file}, normalizeType(type), subPath, true, operatorId,
                Strings.sBlank(StpUtil.getSession().getString("loginname"), ""),
                Strings.sBlank(StpUtil.getSession().getString("username"), ""), tenantId).getFirst();
        return Result.data(result);
    }

    @GetMapping("/view/{tenantId}/{id}")
    @Operation(summary = "公开预览文件")
    public void view(@Parameter(description = "租户ID") @PathVariable String tenantId,
                     @Parameter(description = "文件ID") @PathVariable String id,
                     HttpServletResponse response) {
        fileInfoService.preview(id, Strings.sBlank(tenantId, GlobalConstant.TENANT_ID_DEFAULT), response, true);
    }

    @GetMapping("/view/{id}")
    @Operation(summary = "公开预览默认租户文件")
    public void viewDefault(@Parameter(description = "文件ID") @PathVariable String id, HttpServletResponse response) {
        fileInfoService.preview(id, GlobalConstant.TENANT_ID_DEFAULT, response, true);
    }

    private String normalizeType(String type) {
        if ("blob".equalsIgnoreCase(type)) {
            return "image";
        }
        return Strings.sBlank(type, "file").toLowerCase();
    }
}

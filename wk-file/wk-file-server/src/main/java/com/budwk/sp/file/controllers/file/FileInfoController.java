package com.budwk.sp.file.controllers.file;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.file.dto.FileInfoQueryDTO;
import com.budwk.sp.file.dto.FilePublicFlagDTO;
import com.budwk.sp.file.dto.FileUploadResultDTO;
import com.budwk.sp.file.entity.File_info;
import com.budwk.sp.file.service.FileInfoService;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.nutz.lang.Strings;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/file")
@Tag(name = "文件管理", description = "文件管理接口")
public class FileInfoController {
    private final FileInfoService fileInfoService;

    public FileInfoController(FileInfoService fileInfoService) {
        this.fileInfoService = fileInfoService;
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询文件")
    @SaCheckPermission("sys.manage.file")
    public Result<?> list(@RequestBody(required = false) FileInfoQueryDTO dto) {
        return Result.data(fileInfoService.list(dto, currentTenantId()));
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取文件详情")
    @SaCheckPermission("sys.manage.file.view")
    public Result<File_info> get(@Parameter(description = "文件ID") @PathVariable String id) {
        return Result.data(fileInfoService.getFileInfo(id, currentTenantId()));
    }

    @PostMapping("/upload")
    @Operation(summary = "后台上传文件")
    @SaCheckPermission("sys.manage.file.upload")
    public Result<List<FileUploadResultDTO>> upload(
            @Parameter(description = "上传文件，支持多选") @RequestParam("Filedata") MultipartFile[] files,
            @Parameter(description = "上传分类") @RequestParam(defaultValue = "file") String category,
            @Parameter(description = "租户内子目录") @RequestParam(required = false) String subPath) {
        return Result.data(fileInfoService.upload(files, category, subPath, true, StpUtil.getLoginIdAsString(),
                currentLoginname(), currentUsername(), currentTenantId()));
    }

    @GetMapping("/preview/{id}")
    @Operation(summary = "受权预览文件")
    @SaCheckPermission("sys.manage.file.view")
    public void preview(@Parameter(description = "文件ID") @PathVariable String id, HttpServletResponse response) {
        fileInfoService.preview(id, currentTenantId(), response, false);
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "下载文件")
    @SaCheckPermission("sys.manage.file.download")
    public void download(@Parameter(description = "文件ID") @PathVariable String id, HttpServletResponse response) {
        fileInfoService.download(id, currentTenantId(), response);
    }

    @PostMapping("/public/{id}")
    @Operation(summary = "修改文件公开状态")
    @SaCheckPermission("sys.manage.file")
    public Result<?> updatePublicFlag(@Parameter(description = "文件ID") @PathVariable String id,
                                      @RequestBody @Valid FilePublicFlagDTO dto) {
        fileInfoService.updatePublicFlag(id, dto.getPublicFlag(), StpUtil.getLoginIdAsString(), currentTenantId());
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除文件")
    @SaCheckPermission("sys.manage.file.delete")
    public Result<?> delete(@Parameter(description = "文件ID") @PathVariable String id) {
        fileInfoService.deleteFile(id, currentTenantId());
        return Result.success();
    }

    private String currentTenantId() {
        return Strings.sBlank(StpUtil.getSession().getString("tenantId"), GlobalConstant.TENANT_ID_DEFAULT);
    }

    private String currentLoginname() {
        return Strings.sBlank(StpUtil.getSession().getString("loginname"), "");
    }

    private String currentUsername() {
        return Strings.sBlank(StpUtil.getSession().getString("username"), "");
    }
}

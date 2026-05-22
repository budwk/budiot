package com.budwk.sp.sys.controllers.sys;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.excel.utils.ExcelUtil;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import com.budwk.sp.sys.dto.SysPostDTO;
import com.budwk.sp.sys.entity.Sys_post;
import com.budwk.sp.sys.services.SysPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author wizzer@qq.com
 */
@Slf4j
@RestController
@RequestMapping("/sys/post")
@SLog(tag = "职务管理")
@Tag(name = "职务管理", description = "职务管理接口")
public class SysPostController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SysPostController.class);

    @Autowired
    private SysPostService sysPostService;

    @PostMapping("/list")
    @Operation(summary = "分页查询")
    @SaCheckPermission("sys.manage.post")
    public Result<?> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNo,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String pageOrderName,
            @Parameter(description = "排序方式") @RequestParam(required = false) String pageOrderBy) {
        return Result.data(sysPostService.listPage(pageNo, pageSize, Cnd.NEW().asc("location")));
    }

    @PostMapping("/create")
    @Operation(summary = "新增职务")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.post.create")
    public Result<?> create(@RequestBody @Validated SysPostDTO dto) {
        if (sysPostService.count(Cnd.where("code", "=", dto.getCode())) > 0) {
            return Result.error("职务编号已存在");
        }
        Sys_post post = new Sys_post();
        BeanUtils.copyProperties(dto, post);
        post.setCreatedBy(StpUtil.getLoginIdAsString());
        sysPostService.insert(post);
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "修改职务")
    @RepeatSubmit
    @SaCheckPermission("sys.manage.post.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) SysPostDTO dto) {
        if (sysPostService.count(Cnd.where("code", "=", dto.getCode()).and("id", "<>", dto.getId())) > 0) {
            return Result.error("职务编号已存在");
        }
        Sys_post post = new Sys_post();
        BeanUtils.copyProperties(dto, post);
        post.setUpdatedBy(StpUtil.getLoginIdAsString());
        sysPostService.updateIgnoreNull(post);
        return Result.success();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取职务")
    @SaCheckPermission("sys.manage.post")
    public Result<?> getData(@Parameter(description = "ID") @PathVariable String id) {
        Sys_post post = sysPostService.fetch(id);
        if (post == null) {
            return Result.error("数据不存在");
        }
        return Result.data(post);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除职务")
    @SaCheckPermission("sys.manage.post.delete")
    public Result<?> delete(@Parameter(description = "ID") @PathVariable String id) {
        Sys_post post = sysPostService.fetch(id);
        if (post == null) {
            return Result.error("数据不存在");
        }
        sysPostService.delete(id);
        return Result.success();
    }

    @PostMapping("/location")
    @Operation(summary = "修改职务排序")
    @SaCheckPermission("sys.manage.post.update")
    public Result<?> location(
            @Parameter(description = "排序序号") @RequestParam int location,
            @Parameter(description = "主键ID") @RequestParam String id) {
        sysPostService.update(Chain.make("location", location), Cnd.where("id", "=", id));
        return Result.success();
    }

    @PostMapping("/import")
    @SaCheckLogin
    @Operation(summary = "导入职务数据")
    public Result<?> importData(
            @Parameter(description = "导入文件") @RequestParam("Filedata") MultipartFile file,
            @Parameter(description = "是否覆盖") @RequestParam(defaultValue = "false") boolean cover) {
        if (file == null || file.isEmpty()) {
            return Result.error("未上传文件");
        }
        String filename = file.getOriginalFilename();
        String suffixName = filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf(".")).toLowerCase()
                : "";
        if (!".xls".equalsIgnoreCase(suffixName) && !".xlsx".equalsIgnoreCase(suffixName)) {
            return Result.error("请上传.xls/.xlsx格式文件");
        }
        try {
            ExcelUtil<SysPostDTO> util = new ExcelUtil<>(SysPostDTO.class);
            List<SysPostDTO> list = util.importExcel(file.getInputStream());
            sysPostService.importData(
                    filename,
                    list,
                    cover,
                    StpUtil.getLoginIdAsString(),
                    StpUtil.getSession().getString("loginname")
            );
            return Result.success("文件上传成功，处理结果将通过站内信通知");
        } catch (Exception e) {
            log.error("导入职务数据失败", e);
            throw new BaseException("文件处理失败");
        }
    }
}

package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.enums.DeviceType;
import com.budwk.app.iot.enums.IotPlatform;
import com.budwk.app.iot.enums.ProtocolType;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product;
import com.budwk.app.iot.services.IotClassifyService;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.iot.services.IotProductService;
import com.budwk.app.iot.services.IotProtocolService;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.page.PageUtil;
import com.budwk.starter.common.page.Pagination;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.excel.utils.ExcelUtil;
import com.budwk.starter.log.annotation.SLog;
import com.budwk.starter.security.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.*;
import org.nutz.mvc.impl.AdaptorErrorContext;
import org.nutz.mvc.upload.TempFile;
import org.nutz.mvc.upload.UploadAdaptor;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@IocBean
@At("/iot/admin/device/product")
@SLog(tag = "产品管理")
@ApiDefinition(tag = "产品管理")
@Slf4j
public class IotProductController {

    @Inject
    private IotClassifyService iotClassifyService;

    @Inject
    private IotProtocolService iotProtocolService;

    @Inject
    private IotProductService iotProductService;

    @Inject
    private IotDeviceService iotDeviceService;

    @At
    @Ok("json")
    @GET
    @ApiOperation(name = "获取初始化数据")
    @ApiImplicitParams
    @ApiResponses(
            value = {
                    @ApiResponse(name = "typeList", description = "设备类型"),
                    @ApiResponse(name = "supplierList", description = "厂家列表"),
                    @ApiResponse(name = "iotPlatform", description = "接入平台")
            }
    )
    @SaCheckLogin
    public Result<?> init(HttpServletRequest req) {
        NutMap map = NutMap.NEW();
        map.addv("classifyList", iotClassifyService.query(Cnd.NEW()));
        map.addv("protocolList", iotProtocolService.query(Cnd.NEW()));
        map.addv("iotPlatform", IotPlatform.values());
        map.addv("protocolType", ProtocolType.values());
        map.addv("deviceType", DeviceType.values());
        return Result.success(map);
    }

    @At
    @Ok("json")
    @POST
    @ApiOperation(name = "分页查询")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "classifyId", example = "", description = "设备分类"),
                    @ApiFormParam(name = "name", example = "", description = "产品名称")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> list(@Param("classifyId") String classifyId, @Param("name") String name, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(classifyId)) {
            cnd.and("classifyId", "=", classifyId);
        }
        if (Strings.isNotBlank(name)) {
            cnd.and(Cnd.likeEX("name", name));
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(iotProductService.listPage(pageNo, pageSize, cnd));
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.create")
    @SLog(value = "新增产品:${product.name}")
    @ApiOperation(name = "新增产品")
    @ApiFormParams(
            implementation = Iot_product.class
    )
    @ApiResponses
    public Result<?> create(@Param("..") Iot_product product, HttpServletRequest req) {
        product.setCreatedBy(SecurityUtil.getUserId());
        product.setUpdatedBy(SecurityUtil.getUserId());
        iotProductService.insert(product);
        return Result.success();
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.update")
    @SLog(value = "修改产品:${product.name}")
    @ApiOperation(name = "修改产品")
    @ApiFormParams(
            implementation = Iot_product.class
    )
    @ApiResponses
    public Result<?> update(@Param("..") Iot_product product, HttpServletRequest req) {
        product.setUpdatedBy(SecurityUtil.getUserId());
        iotProductService.updateIgnoreNull(product);
        return Result.success();
    }

    @At
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.delete")
    @SLog(value = "删除产品:${name}")
    @ApiOperation(name = "删除产品")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "name", description = "产品名称")
            }
    )
    @ApiResponses
    public Result<?> delete(@Param("id") String id, @Param("name") String name, HttpServletRequest req) {
        iotProductService.delete(id);
        iotDeviceService.clear(Cnd.where("productId", "=", id));
        return Result.success();
    }

    @At("/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取产品信息")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> get(String id, HttpServletRequest req) {
        Iot_product product = iotProductService.fetch(id);
        iotProductService.fetchLinks(product, "^(protocol|classify)$");
        return Result.success(product);
    }

    @At("/device/count")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product")
    @ApiOperation(name = "获取产品下设备数量")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "ids", description = "产品ID数组")
            }
    )
    @ApiResponses
    public Result<?> deviceCount(@Param("ids") String[] ids, HttpServletRequest req) {
        if (ids == null) {
            return Result.success(NutMap.NEW());
        }
        Sql sql = Sqls.create("select productId,count(id) as total from iot_device where productId in (@ids) group by productId");
        sql.setParam("ids", ids);
        return Result.success(iotProductService.getMap(sql));
    }

    @At("/device/countMore")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product")
    @ApiOperation(name = "获取产品下设备数量")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "产品ID")
            }
    )
    @ApiResponses
    public Result<?> countMore(@Param("id") String id, HttpServletRequest req) {
        int total = iotDeviceService.count(Cnd.where("productId", "=", id));
        int online = iotDeviceService.count(Cnd.where("productId", "=", id).and("online", "=", true));
        int abnormal = iotDeviceService.count(Cnd.where("productId", "=", id).and("abnormal", "=", true));
        return Result.success(
                NutMap.NEW().addv("online", online)
                        .addv("total", total)
                        .addv("abnormal", abnormal)
        );
    }

    @At("/device/import")
    @POST
    @Ok("json:full")
    @AdaptBy(type = UploadAdaptor.class, args = {"ioc:fileUpload"})
    @SaCheckLogin
    @ApiOperation(name = "导入设备数据")
    @SLog(value = "导入设备数据")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "Filedata", example = "", description = "文件表单对象名"),
                    @ApiFormParam(name = "cover", example = "", description = "是否覆盖"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
            },
            mediaType = "multipart/form-data"
    )
    @ApiResponses
    public Result<?> deviceImport(@Param("Filedata") TempFile tf, @Param(value = "cover", df = "false") boolean cover, @Param("productId") String productId, HttpServletRequest req, AdaptorErrorContext err) {
        if (err != null && err.getAdaptorErr() != null) {
            return Result.error("上传文件错误");
        } else if (tf == null) {
            return Result.error("未上传文件");
        } else {
            String suffixName = tf.getSubmittedFileName().substring(tf.getSubmittedFileName().lastIndexOf(".")).toLowerCase();
            if (!".xls".equalsIgnoreCase(suffixName) && !".xlsx".equalsIgnoreCase(suffixName)) {
                return Result.error("请上传.xls/.xlsx格式文件");
            }
            try {
                ExcelUtil<Iot_device> deviceExcelUtil = new ExcelUtil<>(Iot_device.class);
                List<Iot_device> list = deviceExcelUtil.importExcel(tf.getInputStream());
                iotDeviceService.importData(productId, tf.getSubmittedFileName(), list, cover, SecurityUtil.getUserId(), SecurityUtil.getUserLoginname());
                return Result.success("文件上传成功，处理结果将通过站内信通知");
            } catch (Exception e) {
                e.printStackTrace();
                throw new BaseException("文件处理失败");
            }
        }
    }

    @At("/device/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "设备列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "filedName", example = "", description = "查询字段"),
                    @ApiFormParam(name = "filedValue", example = "", description = "字段值")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> deviceList(@Param("productId") String productId, @Param("filedName") String filedName, @Param("filedValue") String filedValue, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(productId)) {
            cnd.and("productId", "=", productId);
        }
        if (Strings.isNotBlank(filedValue)) {
            cnd.and(Cnd.likeEX(filedName, filedValue));
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        return Result.data(iotDeviceService.listPage(pageNo, pageSize, cnd));
    }
}

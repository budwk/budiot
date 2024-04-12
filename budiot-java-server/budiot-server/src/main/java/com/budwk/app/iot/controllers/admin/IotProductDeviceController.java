package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_device_prop;
import com.budwk.app.iot.services.*;
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
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.*;
import org.nutz.mvc.impl.AdaptorErrorContext;
import org.nutz.mvc.upload.TempFile;
import org.nutz.mvc.upload.UploadAdaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@IocBean
@At("/iot/admin/device/product/device")
@SLog(tag = "产品-设备列表")
@ApiDefinition(tag = "产品-设备列表")
@Slf4j
public class IotProductDeviceController {
    @Inject
    private IotClassifyService iotClassifyService;
    @Inject
    private IotProtocolService iotProtocolService;
    @Inject
    private IotProductService iotProductService;
    @Inject
    private IotDeviceService iotDeviceService;
    @Inject
    private IotProductPropService iotProductPropService;
    @Inject
    private IotDevicePropService iotDevicePropService;
    @Inject
    private IotDeviceLogService iotDeviceLogService;

    @At("/count")
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

    @At("/countMore")
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

    @At("/import")
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

    @At("/list")
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

    @At("/create")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.device.create")
    @SLog(value = "新增设备:${device.deviceNo}")
    @ApiOperation(name = "新增设备")
    @ApiFormParams(
            implementation = Iot_device.class
    )
    @ApiResponses
    public Result<?> createDevice(@Param("..") Iot_device device, @Param("props") Map<String, String> props, HttpServletRequest req) {
        iotDeviceService.create(device, props);
        return Result.success();
    }

    @At("/update")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.device.update")
    @SLog(value = "修改设备:${device.name}")
    @ApiOperation(name = "修改设备")
    @ApiFormParams(
            implementation = Iot_device.class
    )
    @ApiResponses
    public Result<?> update(@Param("..") Iot_device device, @Param("props") Map<String, String> props, HttpServletRequest req) {
        device.setUpdatedBy(SecurityUtil.getUserId());
        iotDeviceService.updateIgnoreNull(device);
        Iot_device_prop dbProp = iotDevicePropService.fetch(device.getId());
        if (dbProp == null) {
            Iot_device_prop prop = new Iot_device_prop();
            prop.setDeviceId(device.getId());
            prop.setProperties(props);
            iotDevicePropService.insert(prop);
        } else {
            dbProp.setProperties(props);
            iotDevicePropService.update(dbProp);
        }
        iotDeviceLogService.log("修改设备", device.getId(), SecurityUtil.getUserId(), SecurityUtil.getUserLoginname());
        return Result.success();
    }

    @At("/delete")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.device.delete")
    @SLog(value = "删除设备:${deviceNo}")
    @ApiOperation(name = "删除设备")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "deviceNo", description = "设备通信号")
            }
    )
    @ApiResponses
    public Result<?> deleteDevice(@Param("id") String id, @Param("deviceNo") String deviceNo, HttpServletRequest req) {
        iotDeviceService.delete(iotDeviceService.fetch(id));
        return Result.success();
    }

    @At("/deletes")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.device.delete")
    @SLog(value = "批量删除设备:${ids}")
    @ApiOperation(name = "批量删除设备")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "ids", description = "ID数组")
            }
    )
    @ApiResponses
    public Result<?> deleteDevices(@Param("ids") String[] ids, HttpServletRequest req) {
        for (String id : ids) {
            iotDeviceService.delete(iotDeviceService.fetch(id));
        }
        return Result.success();
    }

    @At("/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取设备信息")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getDevice(String id, HttpServletRequest req) {
        NutMap map = Lang.obj2nutmap(iotDeviceService.fetch(id));
        Iot_device_prop dbProp = iotDevicePropService.fetch(id);
        if (dbProp != null) {
            map.addv("props", dbProp.getProperties());
        }
        return Result.success(map);
    }

    @At("/prop/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取设备属性")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "产品ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getProductProp(String id, HttpServletRequest req) {
        return Result.success(iotProductPropService.query(Cnd.where("productId", "=", id)));
    }

    @At("/export")
    @Ok("void")
    @POST
    @ApiOperation(name = "导出设备数据")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "productId", description = "产品ID"),
                    @ApiFormParam(name = "fieldNames", description = "字段名"),
                    @ApiFormParam(name = "ids", description = "数据ID数组")
            }
    )
    @ApiResponses
    @SaCheckPermission("iot.device.product.device.export")
    public void deviceExport(@Param("ids") String[] ids, @Param("fieldNames") String[] fieldNames, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy, HttpServletRequest req, HttpServletResponse response) {
        Cnd cnd = Cnd.NEW();
        if (ids != null) {
            cnd.and("id", "in", ids);
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        try {
            List<Iot_device> list = iotDeviceService.query(cnd);
            ExcelUtil<Iot_device> util = new ExcelUtil<>(Iot_device.class);
            util.exportExcel(response, list, "设备数据", fieldNames);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}

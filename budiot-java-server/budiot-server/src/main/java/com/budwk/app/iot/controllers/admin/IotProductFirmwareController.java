package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.models.Iot_product_firmware;
import com.budwk.app.iot.models.Iot_product_firmware_device;
import com.budwk.app.iot.services.IotDeviceService;
import com.budwk.app.iot.services.IotProductFirmwareDeviceService;
import com.budwk.app.iot.services.IotProductFirmwareService;
import com.budwk.starter.common.exception.BaseException;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.page.Pagination;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.excel.utils.ExcelUtil;
import com.budwk.starter.log.annotation.SLog;
import com.budwk.starter.security.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.mvc.annotation.*;
import org.nutz.mvc.impl.AdaptorErrorContext;
import org.nutz.mvc.upload.TempFile;
import org.nutz.mvc.upload.UploadAdaptor;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@IocBean
@At("/iot/admin/device/product/firmware")
@SLog(tag = "产品-固件管理")
@ApiDefinition(tag = "产品-固件管理")
@Slf4j
public class IotProductFirmwareController {

    @Inject
    private IotDeviceService iotDeviceService;
    @Inject
    private IotProductFirmwareService iotProductFirmwareService;
    @Inject
    private IotProductFirmwareDeviceService iotProductFirmwareDeviceService;

    @At("/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "产品固件列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> firmwareList(@Param("productId") String productId, @Param("name") String name, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(productId)) {
            cnd.and("productId", "=", productId);
        }
        if (Strings.isNotBlank(name)) {
            cnd.and("name", "like", "%" + name + "%");
        }
        cnd.desc("createdAt");
        return Result.data(iotProductFirmwareService.listPage(pageNo, pageSize, cnd));
    }

    @At("/create")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.firmware")
    @SLog(value = "新增固件:${firmware.name}")
    @ApiOperation(name = "新增固件")
    @ApiFormParams(
            implementation = Iot_product_firmware.class
    )
    @ApiResponses
    public Result<?> firmwareCreate(@Param("..") Iot_product_firmware firmware, HttpServletRequest req) {
        firmware.setCreatedBy(SecurityUtil.getUserId());
        firmware.setUpdatedBy(SecurityUtil.getUserId());
        iotProductFirmwareService.insert(firmware);
        return Result.success();
    }

    @At("/update")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.firmware")
    @SLog(value = "修改固件:${firmware.name}")
    @ApiOperation(name = "修改固件")
    @ApiFormParams(
            implementation = Iot_product_firmware.class
    )
    @ApiResponses
    public Result<?> firmwareUpdate(@Param("..") Iot_product_firmware firmware, HttpServletRequest req) {
        firmware.setUpdatedBy(SecurityUtil.getUserId());
        iotProductFirmwareService.updateIgnoreNull(firmware);
        return Result.success();
    }

    @At("/delete")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.firmware")
    @SLog(value = "删除固件:${name}")
    @ApiOperation(name = "删除固件")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "name", description = "固件名")
            }
    )
    @ApiResponses
    public Result<?> firmwareDelete(@Param("id") String id, @Param("name") String name, HttpServletRequest req) {
        iotProductFirmwareService.deleteFirmware(id);
        return Result.success();
    }

    @At("/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取固件信息")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getFirmware(String id, HttpServletRequest req) {
        return Result.success(iotProductFirmwareService.fetch(id));
    }

    @At("/device/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "固件设备列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "firmwareId", example = "", description = "固件ID"),
                    @ApiFormParam(name = "filedName", example = "", description = "查询字段名"),
                    @ApiFormParam(name = "filedValue", example = "", description = "查询字段值"),
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> firmwareDeviceList(@Param("productId") String productId, @Param("firmwareId") String firmwareId, @Param("filedName") String filedName, @Param("filedValue") String filedValue, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        cnd.and("productId", "=", productId);
        cnd.and("firmwareId", "=", firmwareId);
        if (Strings.isNotBlank(filedName) && Strings.isNotBlank(filedValue)) {
            cnd.and(filedName, "like", "%" + filedValue + "%");
        }
        cnd.desc("createdAt");
        return Result.data(iotProductFirmwareDeviceService.listPage(pageNo, pageSize, cnd));
    }

    @At("/device/insert")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.firmware")
    @SLog(value = "固件绑定设备")
    @ApiOperation(name = "固件绑定设备")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "productId", description = "productId"),
                    @ApiFormParam(name = "firmwareId", description = "firmwareId"),
                    @ApiFormParam(name = "input", description = "输入值")
            }
    )
    @ApiResponses
    public Result<?> firmwareDeviceInsert(@Param("productId") String productId, @Param("firmwareId") String firmwareId, @Param("input") String input, HttpServletRequest req) {
        Cnd cnd = Cnd.NEW();
        cnd.and("productId", "=", productId);
        cnd.and(Cnd.exps("deviceNo", "=", Strings.trim(input)).or("imei", "=", Strings.trim(input)));
        Iot_device device = iotDeviceService.fetch(cnd);
        if (device == null) {
            return Result.error("设备不存在");
        }
        Iot_product_firmware_device firmwareDevice = new Iot_product_firmware_device();
        firmwareDevice.setDeviceId(device.getId());
        firmwareDevice.setDeviceNo(device.getDeviceNo());
        firmwareDevice.setFirmwareId(firmwareId);
        firmwareDevice.setImei(device.getImei());
        firmwareDevice.setProductId(productId);
        firmwareDevice.setCreatedBy(SecurityUtil.getUserId());
        firmwareDevice.setUpdatedBy(SecurityUtil.getUserId());
        iotProductFirmwareDeviceService.insert(firmwareDevice);
        return Result.success();
    }

    @At("/device/delete")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.firmware")
    @ApiOperation(name = "固件解绑设备")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "ids", description = "IDS")
            }
    )
    @ApiResponses
    public Result<?> firmwareDeviceDelete(@Param("ids") String[] ids, HttpServletRequest req) {
        if (Lang.isEmpty(ids)) {
            return Result.error("未传递ID");
        }
        Cnd cnd = Cnd.NEW();
        cnd.and("id", "in", ids);
        iotProductFirmwareDeviceService.clear(cnd);
        return Result.success();
    }

    @At("/device/import")
    @POST
    @Ok("json:full")
    @AdaptBy(type = UploadAdaptor.class, args = {"ioc:fileUpload"})
    @SaCheckPermission("iot.device.product.firmware")
    @ApiOperation(name = "固件导入设备")
    @SLog(value = "固件导入设备")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "Filedata", example = "", description = "文件表单对象名"),
                    @ApiFormParam(name = "cover", example = "", description = "是否覆盖"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "firmwareId", example = "", description = "固件ID"),
            },
            mediaType = "multipart/form-data"
    )
    @ApiResponses
    public Result<?> firmwareImport(@Param("Filedata") TempFile tf, @Param(value = "cover", df = "false") boolean cover,
                                    @Param("productId") String productId, @Param("firmwareId") String firmwareId, HttpServletRequest req, AdaptorErrorContext err) {
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
                ExcelUtil<Iot_product_firmware_device> deviceExcelUtil = new ExcelUtil<>(Iot_product_firmware_device.class);
                List<Iot_product_firmware_device> list = deviceExcelUtil.importExcel(tf.getInputStream());
                String result = iotProductFirmwareDeviceService.importData(productId, firmwareId, tf.getSubmittedFileName(), list, cover, SecurityUtil.getUserId(), SecurityUtil.getUserLoginname());
                return Result.success(result);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BaseException("文件处理失败");
            }
        }
    }
}

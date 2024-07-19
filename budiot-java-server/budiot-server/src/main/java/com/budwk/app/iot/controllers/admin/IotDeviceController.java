package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.access.objects.query.DeviceCmdDataQuery;
import com.budwk.app.access.objects.query.DeviceDataQuery;
import com.budwk.app.access.objects.query.DeviceRawDataQuery;
import com.budwk.app.access.storage.DeviceCmdDataStorage;
import com.budwk.app.access.storage.DeviceDataStorage;
import com.budwk.app.access.storage.DeviceRawDataStorage;
import com.budwk.app.iot.models.Iot_device;
import com.budwk.app.iot.services.*;
import com.budwk.starter.common.openapi.annotation.*;
import com.budwk.starter.common.openapi.enums.ParamIn;
import com.budwk.starter.common.page.PageUtil;
import com.budwk.starter.common.page.Pagination;
import com.budwk.starter.common.result.Result;
import com.budwk.starter.excel.utils.ExcelUtil;
import com.budwk.starter.log.annotation.SLog;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Strings;
import org.nutz.lang.Times;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@IocBean
@At("/iot/admin/device/device")
@SLog(tag = "设备列表")
@ApiDefinition(tag = "设备列表")
@Slf4j
public class IotDeviceController {
    @Inject
    private IotClassifyService iotClassifyService;
    @Inject
    private IotProtocolService iotProtocolService;
    @Inject
    private IotProductService iotProductService;
    @Inject
    private IotProductPropService iotProductPropService;
    @Inject
    private IotProductAttrService iotProductAttrService;
    @Inject
    private IotDeviceService iotDeviceService;
    @Inject
    private DeviceDataStorage deviceDataStorage;
    @Inject
    private DeviceRawDataStorage deviceRawDataStorage;
    @Inject
    private DeviceCmdDataStorage deviceCmdDataStorage;
    @Inject
    private IotDeviceCmdService iotDeviceCmdService;
    @Inject
    private IotProductCmdService iotProductCmdService;

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
                    @ApiFormParam(name = "classifyId", example = "", description = "分类ID"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "filedName", example = "", description = "查询字段"),
                    @ApiFormParam(name = "filedValue", example = "", description = "字段值")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.device")
    public Result<?> list(@Param("classifyId") String classifyId, @Param("productId") String productId, @Param("filedName") String filedName, @Param("filedValue") String filedValue, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(classifyId)) {
            cnd.and("classifyId", "=", classifyId);
        }
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
    @SaCheckPermission("iot.device.device.export")
    public void deviceExport(@Param("ids") String[] ids, @Param("classifyId") String classifyId, @Param("fieldNames") String[] fieldNames, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy, HttpServletRequest req, HttpServletResponse response) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(classifyId)) {
            cnd.and("classifyId", "=", classifyId);
        }
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

    @At("/get/name/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取设备名称")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getName(String id, HttpServletRequest req) {
        return Result.success(iotDeviceService.getField("deviceNo", id));
    }

    @At("/get/ext/{id}")
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
    public Result<?> getExt(String id, HttpServletRequest req) {
        Iot_device device = iotDeviceService.fetch(id);
        iotDeviceService.fetchLinks(device, "^(prop|classify|protocol)$");
        return Result.success(
                NutMap.NEW()
                        .addv("device", device)
                        .addv("productPropList", iotProductPropService.query(Cnd.where("productId", "=", device.getProductId())))
        );
    }

    @At("/data/field/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取设备数据字段")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getFiled(String id, HttpServletRequest req) {
        Iot_device device = iotDeviceService.getField("productId", id);
        if (device == null) {
            return Result.error("设备不存在");
        }
        return Result.success(iotProductAttrService.query(Cnd.where("productId", "=", device.getProductId()).desc("createdAt")));
    }

    @At("/data/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "设备上报数据列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "deviceId", example = "", description = "设备ID"),
                    @ApiFormParam(name = "startTime", example = "", description = "开始时间"),
                    @ApiFormParam(name = "endTime", example = "", description = "结束时间")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckLogin
    public Result<?> dataList(@Param("deviceId") String deviceId, @Param("startTime") String startTime, @Param("endTime") String endTime, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize) {
        Iot_device device = iotDeviceService.getField("protocolCode", deviceId);
        if (device == null) {
            return Result.error("设备不存在");
        }
        DeviceDataQuery deviceDataQuery = new DeviceDataQuery();
        deviceDataQuery.setDeviceId(deviceId);
        deviceDataQuery.setPageNo(pageNo);
        deviceDataQuery.setPageSize(pageSize);
        deviceDataQuery.setProtocolCode(device.getProtocolCode());
        try {
            if (Strings.isNotBlank(startTime)) {
                deviceDataQuery.setStartTime(Times.parse("yyyy-MM-dd HH:mm:ss", startTime + " 00:00:00").getTime());
            }
            if (Strings.isNotBlank(endTime)) {
                deviceDataQuery.setEndTime(Times.parse("yyyy-MM-dd HH:mm:ss", endTime + " 23:59:59").getTime());
            }
        } catch (Exception e) {
            e.getMessage();
        }
        NutMap map = deviceDataStorage.list(deviceDataQuery);
        Pagination pagination = new Pagination();
        pagination.setPageNo(pageNo);
        pagination.setPageSize(pageSize);
        pagination.setList(map.getList("list", NutMap.class));
        pagination.setTotalCount(map.getInt("total"));
        return Result.data(pagination);

    }

    @At("/raw/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "设备原始报文列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "deviceId", example = "", description = "设备ID"),
                    @ApiFormParam(name = "startTime", example = "", description = "开始时间"),
                    @ApiFormParam(name = "endTime", example = "", description = "结束时间")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckLogin
    public Result<?> rawList(@Param("deviceId") String deviceId, @Param("startTime") String startTime, @Param("endTime") String endTime, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize) {
        Iot_device device = iotDeviceService.getField("protocolCode", deviceId);
        if (device == null) {
            return Result.error("设备不存在");
        }
        DeviceRawDataQuery rawDataQuery = new DeviceRawDataQuery();
        rawDataQuery.setDeviceId(deviceId);
        rawDataQuery.setPageNo(pageNo);
        rawDataQuery.setPageSize(pageSize);
        try {
            if (Strings.isNotBlank(startTime)) {
                rawDataQuery.setStartTime(Times.parse("yyyy-MM-dd HH:mm:ss", startTime + " 00:00:00").getTime());
            }
        } catch (Exception e) {
            e.getMessage();
        }
        NutMap map = deviceRawDataStorage.list(rawDataQuery);
        Pagination pagination = new Pagination();
        pagination.setPageNo(pageNo);
        pagination.setPageSize(pageSize);
        pagination.setList(map.getList("list", NutMap.class));
        pagination.setTotalCount(map.getInt("total"));
        return Result.data(pagination);

    }

    @At("/cmd/wait/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "设备待执行指令")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "deviceId", example = "", description = "设备ID"),
                    @ApiFormParam(name = "startTime", example = "", description = "开始时间"),
                    @ApiFormParam(name = "endTime", example = "", description = "结束时间")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckLogin
    public Result<?> cmdWaitList(@Param("deviceId") String deviceId, @Param("startTime") String startTime, @Param("endTime") String endTime, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize) {
        Cnd cnd = Cnd.NEW();
        cnd.and("deviceId", "=", deviceId);
        try {
            if (Strings.isNotBlank(startTime)) {
                cnd.and("createTime", ">=", Times.parse("yyyy-MM-dd HH:mm:ss", startTime + " 00:00:00").getTime());
            }
            if (Strings.isNotBlank(endTime)) {
                cnd.and("createTime", "<=", Times.parse("yyyy-MM-dd HH:mm:ss", endTime + " 00:00:00").getTime());
            }
        } catch (Exception e) {
            e.getMessage();
        }
        cnd.desc("createTime");
        return Result.data(iotDeviceCmdService.listPage(pageNo, pageSize, cnd));
    }

    @At("/cmd/done/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "设备历史指令")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "deviceId", example = "", description = "设备ID"),
                    @ApiFormParam(name = "startTime", example = "", description = "开始时间"),
                    @ApiFormParam(name = "endTime", example = "", description = "结束时间")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckLogin
    public Result<?> cmdDoneList(@Param("deviceId") String deviceId, @Param("startTime") String startTime, @Param("endTime") String endTime, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize) {
        DeviceCmdDataQuery cmdDataQuery = new DeviceCmdDataQuery();
        cmdDataQuery.setDeviceId(deviceId);
        cmdDataQuery.setPageNo(pageNo);
        cmdDataQuery.setPageSize(pageSize);
        try {
            if (Strings.isNotBlank(startTime)) {
                cmdDataQuery.setStartTime(Times.parse("yyyy-MM-dd HH:mm:ss", startTime + " 00:00:00").getTime());
            }
            if (Strings.isNotBlank(endTime)) {
                cmdDataQuery.setEndTime(Times.parse("yyyy-MM-dd HH:mm:ss", endTime + " 00:00:00").getTime());
            }
        } catch (Exception e) {
            e.getMessage();
        }
        NutMap map = deviceCmdDataStorage.list(cmdDataQuery);
        Pagination pagination = new Pagination();
        pagination.setPageNo(pageNo);
        pagination.setPageSize(pageSize);
        pagination.setList(map.getList("list", NutMap.class));
        pagination.setTotalCount(map.getInt("total"));
        return Result.data(pagination);
    }

    @At("/cmd/config/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "设备待执行指令")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "deviceId", example = "", description = "设备ID"),
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckLogin
    public Result<?> cmdConfigList(@Param("deviceId") String deviceId) {
        Iot_device device = iotDeviceService.getField("^(productId|deviceNo)$", deviceId);
        if (device == null) {
            return Result.error("设备不存在");
        }
        Cnd cnd = Cnd.NEW();
        cnd.and("productId", "=", device.getProductId());
        return Result.data(NutMap.NEW().addv("device", device).addv("list", iotProductCmdService.query(cnd, "attrList")));
    }
}

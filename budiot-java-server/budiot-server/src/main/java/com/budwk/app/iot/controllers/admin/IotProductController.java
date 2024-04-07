package com.budwk.app.iot.controllers.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.budwk.app.iot.enums.DeviceType;
import com.budwk.app.iot.enums.IotPlatform;
import com.budwk.app.iot.enums.ProtocolType;
import com.budwk.app.iot.models.*;
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
import org.nutz.dao.Chain;
import org.nutz.dao.Cnd;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.lang.Streams;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;
import org.nutz.mvc.annotation.*;
import org.nutz.mvc.impl.AdaptorErrorContext;
import org.nutz.mvc.upload.TempFile;
import org.nutz.mvc.upload.UploadAdaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
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
    @Inject
    private IotDevicePropService iotDevicePropService;
    @Inject
    private IotDeviceLogService iotDeviceLogService;
    @Inject
    private IotProductAttrService iotProductAttrService;
    @Inject
    private IotProductCmdService iotProductCmdService;
    @Inject
    private IotProductPropService iotProductPropService;
    @Inject
    private IotProductEventService iotProductEventService;
    @Inject
    private IotProductCmdAttrService iotProductCmdAttrService;

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

    @At("/attr/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "参数列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "name", example = "", description = "查询字段")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> attrList(@Param("productId") String productId, @Param("name") String name, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(productId)) {
            cnd.and("productId", "=", productId);
        }
        if (Strings.isNotBlank(name)) {
            cnd.and(Cnd.exps("code", "like", "%" + name + "%").or("name", "like", "%" + name + "%"));
        }
        cnd.asc("location");
        return Result.data(iotProductAttrService.listPage(pageNo, pageSize, cnd));
    }

    @At("/attr/create")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "新增参数:${attr.name}")
    @ApiOperation(name = "新增参数")
    @ApiFormParams(
            implementation = Iot_product_attr.class
    )
    @ApiResponses
    public Result<?> attrCreate(@Param("..") Iot_product_attr attr, HttpServletRequest req) {
        attr.setCreatedBy(SecurityUtil.getUserId());
        attr.setUpdatedBy(SecurityUtil.getUserId());
        iotProductAttrService.insert(attr);
        return Result.success();
    }

    @At("/attr/update")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "修改参数:${attr.name}")
    @ApiOperation(name = "修改参数")
    @ApiFormParams(
            implementation = Iot_product_attr.class
    )
    @ApiResponses
    public Result<?> attrUpdate(@Param("..") Iot_product_attr attr, HttpServletRequest req) {
        attr.setUpdatedBy(SecurityUtil.getUserId());
        iotProductAttrService.updateIgnoreNull(attr);
        return Result.success();
    }

    @At("/attr/delete")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "删除参数:${name}")
    @ApiOperation(name = "删除参数")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "name", description = "参数名")
            }
    )
    @ApiResponses
    public Result<?> attrDelete(@Param("id") String id, @Param("name") String name, HttpServletRequest req) {
        iotProductAttrService.delete(id);
        return Result.success();
    }

    @At("/attr/sort")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @ApiOperation(name = "参数排序")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "ids", description = "ID数组"),
                    @ApiFormParam(name = "pageNo", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", description = "页大小", type = "integer")
            }
    )
    @ApiResponses
    public Result<?> attrSort(@Param("ids") String[] ids, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, HttpServletRequest req) {
        int location = (pageNo - 1) * pageSize;
        for (String id : ids) {
            location++;
            iotProductAttrService.update(Chain.make("location", location), Cnd.where("id", "=", id));
        }
        return Result.success();
    }

    @At("/attr/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取参数")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getAttr(String id, HttpServletRequest req) {
        return Result.success(iotProductAttrService.fetch(id));
    }


    @At("/attr/export")
    @Ok("void")
    @POST
    @ApiOperation(name = "导出参数")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "productId", description = "产品ID"),
                    @ApiFormParam(name = "fieldNames", description = "字段名"),
                    @ApiFormParam(name = "ids", description = "ID数组")
            }
    )
    @ApiResponses
    @SaCheckPermission("iot.device.product.config")
    public void attrExport(@Param("ids") String[] ids, @Param("fieldNames") String[] fieldNames, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy, HttpServletRequest req, HttpServletResponse response) {
        Cnd cnd = Cnd.NEW();
        if (ids != null) {
            cnd.and("id", "in", ids);
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        try {
            List<Iot_product_attr> list = iotProductAttrService.query(cnd);
            ExcelUtil<Iot_product_attr> util = new ExcelUtil<>(Iot_product_attr.class);
            util.exportExcel(response, list, "参数数据", fieldNames);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @At("/attr/import")
    @POST
    @Ok("json:full")
    @AdaptBy(type = UploadAdaptor.class, args = {"ioc:fileUpload"})
    @SaCheckPermission("iot.device.product.config")
    @ApiOperation(name = "导入参数")
    @SLog(value = "导入参数")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "Filedata", example = "", description = "文件表单对象名"),
                    @ApiFormParam(name = "cover", example = "", description = "是否覆盖"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
            },
            mediaType = "multipart/form-data"
    )
    @ApiResponses
    public Result<?> attrImport(@Param("Filedata") TempFile tf, @Param(value = "cover", df = "false") boolean cover, @Param("productId") String productId, HttpServletRequest req, AdaptorErrorContext err) {
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
                ExcelUtil<Iot_product_attr> deviceExcelUtil = new ExcelUtil<>(Iot_product_attr.class);
                List<Iot_product_attr> list = deviceExcelUtil.importExcel(tf.getInputStream());
                String result = iotProductAttrService.importData(productId, tf.getSubmittedFileName(), list, cover, SecurityUtil.getUserId(), SecurityUtil.getUserLoginname());
                return Result.success(result);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BaseException("文件处理失败");
            }
        }
    }

    @At("/prop/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "属性列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "name", example = "", description = "查询字段")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> propList(@Param("productId") String productId, @Param("name") String name, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(productId)) {
            cnd.and("productId", "=", productId);
        }
        if (Strings.isNotBlank(name)) {
            cnd.and(Cnd.exps("code", "like", "%" + name + "%").or("name", "like", "%" + name + "%"));
        }
        cnd.asc("location");
        return Result.data(iotProductPropService.listPage(pageNo, pageSize, cnd));
    }

    @At("/prop/create")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "新增属性:${prop.name}")
    @ApiOperation(name = "新增属性")
    @ApiFormParams(
            implementation = Iot_product_prop.class
    )
    @ApiResponses
    public Result<?> attrCreate(@Param("..") Iot_product_prop prop, HttpServletRequest req) {
        prop.setCreatedBy(SecurityUtil.getUserId());
        prop.setUpdatedBy(SecurityUtil.getUserId());
        iotProductPropService.insert(prop);
        return Result.success();
    }

    @At("/prop/update")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "修改属性:${prop.name}")
    @ApiOperation(name = "修改属性")
    @ApiFormParams(
            implementation = Iot_product_attr.class
    )
    @ApiResponses
    public Result<?> propUpdate(@Param("..") Iot_product_prop prop, HttpServletRequest req) {
        prop.setUpdatedBy(SecurityUtil.getUserId());
        iotProductPropService.updateIgnoreNull(prop);
        return Result.success();
    }

    @At("/prop/delete")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "删除属性:${name}")
    @ApiOperation(name = "删除属性")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "name", description = "属性名")
            }
    )
    @ApiResponses
    public Result<?> propDelete(@Param("id") String id, @Param("name") String name, HttpServletRequest req) {
        iotProductPropService.delete(id);
        return Result.success();
    }

    @At("/prop/sort")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @ApiOperation(name = "属性排序")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "ids", description = "ID数组"),
                    @ApiFormParam(name = "pageNo", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", description = "页大小", type = "integer")
            }
    )
    @ApiResponses
    public Result<?> propSort(@Param("ids") String[] ids, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, HttpServletRequest req) {
        int location = (pageNo - 1) * pageSize;
        for (String id : ids) {
            location++;
            iotProductPropService.update(Chain.make("location", location), Cnd.where("id", "=", id));
        }
        return Result.success();
    }

    @At("/prop/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取属性")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getProp(String id, HttpServletRequest req) {
        return Result.success(iotProductPropService.fetch(id));
    }

    @At("/event/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "事件配置列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "name", example = "", description = "查询字段")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> eventList(@Param("productId") String productId, @Param("name") String name, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(productId)) {
            cnd.and("productId", "=", productId);
        }
        if (Strings.isNotBlank(name)) {
            cnd.and(Cnd.exps("code", "like", "%" + name + "%").or("name", "like", "%" + name + "%"));
        }
        return Result.data(iotProductEventService.listPage(pageNo, pageSize, cnd));
    }

    @At("/event/create")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "新增事件配置:${event.name}")
    @ApiOperation(name = "新增事件配置")
    @ApiFormParams(
            implementation = Iot_product_event.class
    )
    @ApiResponses
    public Result<?> eventCreate(@Param("..") Iot_product_event event, HttpServletRequest req) {
        event.setCreatedBy(SecurityUtil.getUserId());
        event.setUpdatedBy(SecurityUtil.getUserId());
        iotProductEventService.insert(event);
        return Result.success();
    }

    @At("/event/update")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "修改事件配置:${event.name}")
    @ApiOperation(name = "修改事件配置")
    @ApiFormParams(
            implementation = Iot_product_event.class
    )
    @ApiResponses
    public Result<?> eventUpdate(@Param("..") Iot_product_event event, HttpServletRequest req) {
        event.setUpdatedBy(SecurityUtil.getUserId());
        iotProductEventService.updateIgnoreNull(event);
        return Result.success();
    }

    @At("/event/delete")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "删除事件配置:${name}")
    @ApiOperation(name = "删除事件配置")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "name", description = "事件名")
            }
    )
    @ApiResponses
    public Result<?> eventDelete(@Param("id") String id, @Param("name") String name, HttpServletRequest req) {
        iotProductEventService.delete(id);
        return Result.success();
    }

    @At("/event/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取事件配置")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getEvent(String id, HttpServletRequest req) {
        return Result.success(iotProductEventService.fetch(id));
    }

    @At("/cmd/list")
    @Ok("json")
    @POST
    @ApiOperation(name = "指令配置列表")
    @ApiFormParams(
            {
                    @ApiFormParam(name = "pageNo", example = "1", description = "页码", type = "integer"),
                    @ApiFormParam(name = "pageSize", example = "10", description = "页大小", type = "integer"),
                    @ApiFormParam(name = "pageOrderName", example = "createdAt", description = "排序字段"),
                    @ApiFormParam(name = "pageOrderBy", example = "descending", description = "排序方式"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
                    @ApiFormParam(name = "name", example = "", description = "查询字段")
            }
    )
    @ApiResponses(
            implementation = Pagination.class
    )
    @SaCheckPermission("iot.device.product")
    public Result<?> cmdList(@Param("productId") String productId, @Param("name") String name, @Param("pageNo") int pageNo, @Param("pageSize") int pageSize, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy) {
        Cnd cnd = Cnd.NEW();
        if (Strings.isNotBlank(productId)) {
            cnd.and("productId", "=", productId);
        }
        if (Strings.isNotBlank(name)) {
            cnd.and(Cnd.exps("code", "like", "%" + name + "%").or("name", "like", "%" + name + "%"));
        }
        return Result.data(iotProductCmdService.listPageLinks(pageNo, pageSize, cnd, "attrList"));
    }

    @At("/cmd/create")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "新增指令:${cmd.name}")
    @ApiOperation(name = "新增指令")
    @ApiFormParams(
            implementation = Iot_product_cmd.class
    )
    @ApiResponses
    public Result<?> cmdCreate(@Param("..") Iot_product_cmd cmd, HttpServletRequest req) {
        cmd.setCreatedBy(SecurityUtil.getUserId());
        cmd.setUpdatedBy(SecurityUtil.getUserId());
        iotProductCmdService.insertLinks(iotProductCmdService.insert(cmd), "attrList");
        return Result.success();
    }

    @At("/cmd/update")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "修改指令:${cmd.name}")
    @ApiOperation(name = "修改指令")
    @ApiFormParams(
            implementation = Iot_product_cmd.class
    )
    @ApiResponses
    public Result<?> cmdUpdate(@Param("..") Iot_product_cmd cmd, HttpServletRequest req) {
        cmd.setUpdatedBy(SecurityUtil.getUserId());
        iotProductCmdService.updateIgnoreNull(cmd);
        iotProductCmdAttrService.clear(Cnd.where("cmdId", "=", cmd.getId()));
        iotProductCmdService.insertLinks(cmd, "attrList");
        return Result.success();
    }

    @At("/cmd/delete")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @SLog(value = "删除指令:${name}")
    @ApiOperation(name = "删除指令")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "name", description = "指令名")
            }
    )
    @ApiResponses
    public Result<?> cmdDelete(@Param("id") String id, @Param("name") String name, HttpServletRequest req) {
        iotProductCmdService.delete(id);
        iotProductCmdAttrService.clear(Cnd.where("cmdId", "=", id));
        return Result.success();
    }

    @At("/cmd/get/{id}")
    @GET
    @Ok("json")
    @SaCheckLogin
    @ApiOperation(name = "获取指令")
    @ApiImplicitParams(
            {
                    @ApiImplicitParam(name = "id", description = "主键ID", in = ParamIn.PATH, required = true, check = true)
            }
    )
    @ApiResponses
    public Result<?> getCmd(String id, HttpServletRequest req) {
        return Result.success(iotProductCmdService.fetchLinks(iotProductCmdService.fetch(id), "attrList"));
    }

    @At("/cmd/enabled")
    @POST
    @Ok("json")
    @SaCheckPermission("iot.device.product.config")
    @ApiOperation(name = "启用禁用指令")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "id", description = "ID"),
                    @ApiFormParam(name = "name", description = "指令名")
            }
    )
    @ApiResponses
    public Result<?> cmdEnabled(@Param("id") String id, @Param("name") String name, @Param("enabled") boolean enabled, HttpServletRequest req) {
        iotProductCmdService.update(Chain.make("enabled", enabled), Cnd.where("id", "=", id));
        return Result.success();
    }

    @At("/cmd/export")
    @Ok("void")
    @POST
    @ApiOperation(name = "导出指令")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "productId", description = "产品ID"),
                    @ApiFormParam(name = "fieldNames", description = "字段名"),
                    @ApiFormParam(name = "ids", description = "ID数组")
            }
    )
    @ApiResponses
    @SaCheckPermission("iot.device.product.config")
    public void cmdExport(@Param("ids") String[] ids, @Param("fieldNames") String[] fieldNames, @Param("pageOrderName") String pageOrderName, @Param("pageOrderBy") String pageOrderBy, HttpServletRequest req, HttpServletResponse response) {
        Cnd cnd = Cnd.NEW();
        if (ids != null) {
            cnd.and("id", "in", ids);
        }
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) {
            cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        }
        try {
            List<Iot_product_cmd> list = iotProductCmdService.query(cnd, "attrList");
            response.setContentType("text/plain");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;");
            try (PrintWriter writer = response.getWriter()) {
                JsonFormat jsonFormat = JsonFormat.full();
                jsonFormat.setLocked("^(id|cmdId|productId|createdAt|createdBy|updatedAt|updatedBy|delFlag)$");
                writer.write(Json.toJson(list, jsonFormat));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @At("/cmd/import")
    @POST
    @Ok("json:full")
    @AdaptBy(type = UploadAdaptor.class, args = {"ioc:fileUpload"})
    @SaCheckPermission("iot.device.product.config")
    @ApiOperation(name = "导入指令")
    @SLog(value = "导入指令")
    @ApiFormParams(
            value = {
                    @ApiFormParam(name = "Filedata", example = "", description = "文件表单对象名"),
                    @ApiFormParam(name = "cover", example = "", description = "是否覆盖"),
                    @ApiFormParam(name = "productId", example = "", description = "产品ID"),
            },
            mediaType = "multipart/form-data"
    )
    @ApiResponses
    public Result<?> cmdImport(@Param("Filedata") TempFile tf, @Param(value = "cover", df = "false") boolean cover, @Param("productId") String productId, HttpServletRequest req, AdaptorErrorContext err) {
        if (err != null && err.getAdaptorErr() != null) {
            return Result.error("上传文件错误");
        } else if (tf == null) {
            return Result.error("未上传文件");
        } else {
            String suffixName = tf.getSubmittedFileName().substring(tf.getSubmittedFileName().lastIndexOf(".")).toLowerCase();
            if (!".json".equalsIgnoreCase(suffixName) && !".txt".equalsIgnoreCase(suffixName)) {
                return Result.error("请上传 json/txt 格式文件");
            }
            try {
                byte[] bytes = Streams.readBytesAndClose(tf.getInputStream());
                String result = iotProductCmdService.importData(productId, new String(bytes), cover, SecurityUtil.getUserId(), SecurityUtil.getUserLoginname());
                return Result.success(result);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BaseException("文件处理失败");
            }
        }
    }
}

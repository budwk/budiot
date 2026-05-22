package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceProductDTO;
import com.budwk.sp.device.dto.DeviceProductThingModelDTO;
import com.budwk.sp.device.entity.Device_category;
import com.budwk.sp.device.entity.Device_gateway;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.entity.Device_vendor;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.enums.DeviceProductType;
import com.budwk.sp.device.services.*;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/iot/product")
@SLog(tag = "设备产品")
@Tag(name = "设备产品", description = "设备产品接口")
public class DeviceProductController {
    private final DeviceProductService deviceProductService;
    private final DeviceCategoryService deviceCategoryService;
    private final DeviceVendorService deviceVendorService;
    private final DeviceProtocolService deviceProtocolService;
    private final DeviceGatewayService deviceGatewayService;

    public DeviceProductController(DeviceProductService deviceProductService, DeviceCategoryService deviceCategoryService, DeviceVendorService deviceVendorService, DeviceProtocolService deviceProtocolService, DeviceGatewayService deviceGatewayService) {
        this.deviceProductService = deviceProductService;
        this.deviceCategoryService = deviceCategoryService;
        this.deviceVendorService = deviceVendorService;
        this.deviceProtocolService = deviceProtocolService;
        this.deviceGatewayService = deviceGatewayService;
    }

    @GetMapping("/data")
    @Operation(summary = "获取产品基础数据")
    @SaCheckPermission("iot.manage.product")
    public Result<?> data() {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Map<String, Object> map = new HashMap<>();
        map.put("productTypes", List.of(DeviceProductType.values()).stream().map(type -> Map.of("text", type.getText(), "value", type.getValue())).toList());
        map.put("networkProtocols", List.of(DeviceNetworkProtocol.values()).stream().map(type -> Map.of("text", type.getText(), "value", type.getValue())).toList());
        map.put("categories", deviceCategoryService.query(Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false).asc("location").asc("path")));
        map.put("vendors", deviceVendorService.listEnabled(tenantId));
        map.put("protocols", deviceProtocolService.listEnabled(tenantId));
        map.put("gatewayNodes", deviceGatewayService.listGatewayNodes(tenantId, null));
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询产品")
    @SaCheckPermission("iot.manage.product")
    public Result<?> list(@RequestParam(required = false) String name,
                          @RequestParam(required = false) String productKey,
                          @RequestParam(required = false) String productType,
                          @RequestParam(required = false) String networkProtocol,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false);
        if (Strings.isNotBlank(name)) cnd.and("name", "like", "%" + name + "%");
        if (Strings.isNotBlank(productKey)) cnd.and("productKey", "like", "%" + productKey + "%");
        if (Strings.isNotBlank(productType)) cnd.and("productType", "=", productType);
        if (Strings.isNotBlank(networkProtocol)) cnd.and("networkProtocol", "=", networkProtocol);
        if (disabled != null) cnd.and("disabled", "=", disabled);
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        else cnd.desc("updatedAt");
        var page = deviceProductService.listPage(pageNo, pageSize, cnd);
        enrich(page.getList(), tenantId);
        return Result.data(page);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取产品详情")
    @SaCheckPermission("iot.manage.product")
    public Result<?> get(@PathVariable String id) {
        Device_product product = deviceProductService.getProduct(id, StpUtil.getSession().getString("tenantId"));
        enrich(List.of(product), StpUtil.getSession().getString("tenantId"));
        return Result.data(product);
    }

    @GetMapping("/thing-model/get/{id}")
    @Operation(summary = "获取产品物模型")
    @SaCheckPermission("iot.manage.product")
    public Result<?> getThingModel(@PathVariable String id) {
        Device_product product = deviceProductService.getProduct(id, StpUtil.getSession().getString("tenantId"));
        Map<String, Object> map = new HashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("productKey", product.getProductKey());
        map.put("thingPropertyJson", Strings.sNull(product.getThingPropertyJson()).trim());
        map.put("thingServiceJson", Strings.sNull(product.getThingServiceJson()).trim());
        map.put("thingEventJson", Strings.sNull(product.getThingEventJson()).trim());
        return Result.data(map);
    }

    @PostMapping("/create")
    @Operation(summary = "新增产品")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.product.create")
    public Result<?> create(@RequestBody @Validated DeviceProductDTO dto) { return Result.data(deviceProductService.createProduct(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/update")
    @Operation(summary = "修改产品")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.product.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) DeviceProductDTO dto) { return Result.data(deviceProductService.updateProduct(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/thing-model/update")
    @Operation(summary = "修改产品物模型")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.product.update")
    public Result<?> updateThingModel(@RequestBody @Validated DeviceProductThingModelDTO dto) {
        return Result.data(deviceProductService.updateThingModel(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId")));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除产品")
    @SaCheckPermission("iot.manage.product.delete")
    public Result<?> delete(@PathVariable String id) { deviceProductService.deleteProduct(id, StpUtil.getSession().getString("tenantId")); return Result.success(); }

    private void enrich(List<?> rows, String tenantId) {
        if (rows == null || rows.isEmpty()) return;
        List<Device_product> list = rows.stream().filter(Device_product.class::isInstance).map(Device_product.class::cast).toList();
        if (list.isEmpty()) return;
        Map<String, Device_category> categoryMap = deviceCategoryService.query(Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false)).stream().collect(Collectors.toMap(Device_category::getId, item -> item, (a, b) -> a));
        Map<String, Device_vendor> vendorMap = deviceVendorService.query(Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false)).stream().collect(Collectors.toMap(Device_vendor::getId, item -> item, (a, b) -> a));
        Map<String, Device_protocol> protocolMap = deviceProtocolService.query(Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false)).stream().collect(Collectors.toMap(Device_protocol::getId, item -> item, (a, b) -> a));
        Map<String, Device_gateway> gatewayMap = deviceGatewayService.getGatewayMap(list.stream().map(Device_product::getGatewayNodeId).filter(Strings::isNotBlank).distinct().toList(), tenantId);
        Map<String, Integer> deviceCountMap = deviceProductService.countDeviceByProductIds(list.stream().map(Device_product::getId).toList(), tenantId);
        for (Device_product product : list) {
            product.setCategory(categoryMap.get(product.getCategoryId()));
            product.setVendor(vendorMap.get(product.getVendorId()));
            product.setProtocol(protocolMap.get(product.getProtocolId()));
            product.setDeviceCount(deviceCountMap.getOrDefault(product.getId(), 0));
            Device_gateway gateway = gatewayMap.get(product.getGatewayNodeId());
            if (gateway != null) {
                product.setGatewayName(gateway.getName());
                product.setGatewayStatus(gateway.getRuntimeStatus() == null ? "" : gateway.getRuntimeStatus().getValue());
                if (product.getGatewayPort() == null) {
                    product.setGatewayPort(gateway.getPort());
                }
            }
        }
    }
}

package com.budwk.sp.device.controllers;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.budwk.sp.device.dto.DeviceBatchCreateDTO;
import com.budwk.sp.device.dto.DeviceInfoDTO;
import com.budwk.sp.device.dto.DeviceProtocolDebugRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolParseResultDTO;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.entity.Device_protocol;
import com.budwk.sp.device.entity.Device_raw_log;
import com.budwk.sp.device.enums.DeviceMessageDirection;
import com.budwk.sp.device.services.DeviceCommandService;
import com.budwk.sp.device.services.DeviceInfoService;
import com.budwk.sp.device.services.DeviceProtocolService;
import com.budwk.sp.device.services.DeviceProductService;
import com.budwk.sp.device.services.DeviceRuntimeService;
import com.budwk.sp.device.services.DeviceTelemetryService;
import com.budwk.sp.starter.common.page.PageUtil;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.common.valid.group.Update;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.budwk.sp.starter.log.annotation.SLog;
import com.budwk.sp.starter.web.repeat.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.nutz.dao.Cnd;
import org.nutz.lang.Strings;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/iot/device")
@SLog(tag = "设备管理")
@Tag(name = "设备管理", description = "设备管理接口")
public class DeviceInfoController {
    private final DeviceInfoService deviceInfoService;
    private final DeviceProductService deviceProductService;
    private final DeviceRuntimeService deviceRuntimeService;
    private final DeviceCommandService deviceCommandService;
    private final DeviceTelemetryService deviceTelemetryService;
    private final DeviceProtocolService deviceProtocolService;
    private final ObjectMapper objectMapper;

    public DeviceInfoController(DeviceInfoService deviceInfoService,
                                DeviceProductService deviceProductService,
                                DeviceRuntimeService deviceRuntimeService,
                                DeviceCommandService deviceCommandService,
                                DeviceTelemetryService deviceTelemetryService,
                                DeviceProtocolService deviceProtocolService,
                                ObjectMapper objectMapper) {
        this.deviceInfoService = deviceInfoService;
        this.deviceProductService = deviceProductService;
        this.deviceRuntimeService = deviceRuntimeService;
        this.deviceCommandService = deviceCommandService;
        this.deviceTelemetryService = deviceTelemetryService;
        this.deviceProtocolService = deviceProtocolService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/data")
    @Operation(summary = "获取设备基础数据")
    @SaCheckPermission("iot.manage.device")
    public Result<?> data() {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Map<String, Object> map = new HashMap<>();
        map.put("products", deviceProductService.listEnabled(tenantId));
        return Result.data(map);
    }

    @PostMapping("/list")
    @Operation(summary = "分页查询设备")
    @SaCheckPermission("iot.manage.device")
    public Result<?> list(@RequestParam(required = false) String deviceCode,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String productId,
                          @RequestParam(required = false) Boolean disabled,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String pageOrderName,
                          @RequestParam(required = false) String pageOrderBy) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false);
        if (Strings.isNotBlank(deviceCode)) cnd.and("deviceCode", "like", "%" + deviceCode + "%");
        if (Strings.isNotBlank(name)) cnd.and("name", "like", "%" + name + "%");
        if (Strings.isNotBlank(productId)) cnd.and("productId", "=", productId);
        if (disabled != null) cnd.and("disabled", "=", disabled);
        if (Strings.isNotBlank(pageOrderName) && Strings.isNotBlank(pageOrderBy)) cnd.orderBy(pageOrderName, PageUtil.getOrder(pageOrderBy));
        else cnd.desc("updatedAt");
        var page = deviceInfoService.listPage(pageNo, pageSize, cnd);
        enrich(page.getList(), tenantId);
        return Result.data(page);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取设备详情")
    @SaCheckPermission("iot.manage.device")
    public Result<?> get(@PathVariable String id) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Device_info info = deviceInfoService.getDevice(id, tenantId);
        enrich(List.of(info), tenantId);
        Map<String, Object> map = new HashMap<>();
        map.put("device", info);
        map.put("runtime", deviceRuntimeService.getRuntime(tenantId, id));
        map.put("rawLogs", deviceTelemetryService.listRawLogs(id, tenantId, 20));
        map.put("dataLogs", deviceTelemetryService.listDataLogs(id, tenantId, 20));
        map.put("eventLogs", deviceTelemetryService.listEventLogs(id, tenantId, 20));
        map.put("pendingCommands", deviceCommandService.listPending(id, tenantId));
        map.put("commandLogs", deviceCommandService.listLogs(id, tenantId));
        map.put("latestProperties", buildLatestProperties(info, tenantId));
        return Result.data(map);
    }

    @GetMapping("/raw/logs")
    @Operation(summary = "分页查询设备通信报文")
    @SaCheckPermission("iot.manage.device")
    public Result<?> rawLogs(@RequestParam String deviceId,
                             @RequestParam(required = false) Long startAt,
                             @RequestParam(required = false) Long endAt,
                             @RequestParam(defaultValue = "1") int pageNo,
                             @RequestParam(defaultValue = "10") int pageSize) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        Device_info info = deviceInfoService.getDevice(deviceId, tenantId);
        enrich(List.of(info), tenantId);
        var page = deviceTelemetryService.pageRawLogs(deviceId, tenantId, startAt, endAt, pageNo, pageSize);
        List<Device_raw_log> rows = page.getList(Device_raw_log.class);
        page.setList(rows.stream().map(log -> toRawLogView(log, info.getProduct(), tenantId)).toList());
        return Result.data(page);
    }

    @GetMapping("/data/logs")
    @Operation(summary = "分页查询设备上报数据")
    @SaCheckPermission("iot.manage.device")
    public Result<?> dataLogs(@RequestParam String deviceId,
                              @RequestParam(required = false) Long startAt,
                              @RequestParam(required = false) Long endAt,
                              @RequestParam(defaultValue = "1") int pageNo,
                              @RequestParam(defaultValue = "10") int pageSize) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        return Result.data(deviceTelemetryService.pageDataLogs(deviceId, tenantId, startAt, endAt, pageNo, pageSize));
    }

    @GetMapping("/event/logs")
    @Operation(summary = "分页查询设备事件数据")
    @SaCheckPermission("iot.manage.device")
    public Result<?> eventLogs(@RequestParam String deviceId,
                               @RequestParam(required = false) Long startAt,
                               @RequestParam(required = false) Long endAt,
                               @RequestParam(defaultValue = "1") int pageNo,
                               @RequestParam(defaultValue = "10") int pageSize) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        return Result.data(deviceTelemetryService.pageEventLogs(deviceId, tenantId, startAt, endAt, pageNo, pageSize));
    }

    @GetMapping("/command/pending")
    @Operation(summary = "分页查询设备待下发指令")
    @SaCheckPermission("iot.manage.device.command")
    public Result<?> pendingCommands(@RequestParam String deviceId,
                                     @RequestParam(required = false) Long startAt,
                                     @RequestParam(required = false) Long endAt,
                                     @RequestParam(defaultValue = "1") int pageNo,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        return Result.data(deviceCommandService.pagePending(deviceId, tenantId, startAt, endAt, pageNo, pageSize));
    }

    @GetMapping("/command/logs")
    @Operation(summary = "分页查询设备历史指令")
    @SaCheckPermission("iot.manage.device.command")
    public Result<?> commandLogs(@RequestParam String deviceId,
                                 @RequestParam(required = false) Long startAt,
                                 @RequestParam(required = false) Long endAt,
                                 @RequestParam(defaultValue = "1") int pageNo,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        String tenantId = StpUtil.getSession().getString("tenantId");
        return Result.data(deviceCommandService.pageLogs(deviceId, tenantId, startAt, endAt, pageNo, pageSize));
    }

    @PostMapping("/create")
    @Operation(summary = "新增设备")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.device.create")
    public Result<?> create(@RequestBody @Validated DeviceInfoDTO dto) { return Result.data(deviceInfoService.createDevice(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/batch_create")
    @Operation(summary = "批量新增设备")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.device.create")
    public Result<?> batchCreate(@RequestBody @Validated DeviceBatchCreateDTO dto) { return Result.data(deviceInfoService.batchCreate(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @PostMapping("/update")
    @Operation(summary = "修改设备")
    @RepeatSubmit
    @SaCheckPermission("iot.manage.device.update")
    public Result<?> update(@RequestBody @Validated({Default.class, Update.class}) DeviceInfoDTO dto) { return Result.data(deviceInfoService.updateDevice(dto, StpUtil.getLoginIdAsString(), StpUtil.getSession().getString("tenantId"))); }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除设备")
    @SaCheckPermission("iot.manage.device.delete")
    public Result<?> delete(@PathVariable String id) { deviceInfoService.deleteDevice(id, StpUtil.getSession().getString("tenantId")); return Result.success(); }

    private void enrich(List<?> rows, String tenantId) {
        if (rows == null || rows.isEmpty()) return;
        List<Device_info> list = rows.stream().filter(Device_info.class::isInstance).map(Device_info.class::cast).toList();
        if (list.isEmpty()) return;
        Map<String, Device_product> productMap = deviceProductService.query(Cnd.where("tenantId", "=", tenantId).and("delFlag", "=", false)).stream().collect(Collectors.toMap(Device_product::getId, item -> item, (a, b) -> a));
        for (Device_info info : list) {
            info.setProduct(productMap.get(info.getProductId()));
            var runtime = deviceRuntimeService.getRuntime(tenantId, info.getId());
            info.setOnline(runtime.isOnline());
            info.setIp(runtime.getIp());
            info.setLastHeartbeatAt(runtime.getLastHeartbeatAt());
            info.setLastDeviceAt(runtime.getLastDeviceAt());
            info.setGatewayNodeId(runtime.getGatewayNodeId());
        }
    }

    private List<Map<String, Object>> buildLatestProperties(Device_info info, String tenantId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (info == null || info.getProduct() == null) {
            return rows;
        }
        Map<String, Map<String, Object>> latestMap = new LinkedHashMap<>();
        for (Map<String, Object> dataRow : deviceTelemetryService.listDataLogs(info.getId(), tenantId, 200)) {
            Object properties = dataRow.get("properties");
            if (!(properties instanceof Map<?, ?> propertyMap)) {
                continue;
            }
            for (Map.Entry<?, ?> entry : propertyMap.entrySet()) {
                String identifier = Strings.sNull(String.valueOf(entry.getKey())).trim();
                if (Strings.isBlank(identifier) || latestMap.containsKey(identifier) || !(entry.getValue() instanceof Map<?, ?> valueMap)) {
                    continue;
                }
                latestMap.put(identifier, new LinkedHashMap<>((Map<String, Object>) valueMap));
            }
        }
        for (Map<String, Object> property : parseThingPropertyList(info.getProduct().getThingPropertyJson())) {
            String identifier = readMapString(property, "identifier");
            if (Strings.isBlank(identifier)) {
                continue;
            }
            Map<String, Object> log = latestMap.remove(identifier);
            rows.add(toLatestPropertyRow(identifier, property, log));
        }
        latestMap.forEach((identifier, log) -> rows.add(toLatestPropertyRow(identifier, Map.of(), log)));
        return rows;
    }

    private Map<String, Object> toLatestPropertyRow(String identifier, Map<String, Object> property, Map<String, Object> log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("identifier", identifier);
        row.put("name", Strings.sBlank(readMapString(property, "name"), readMapString(log, "name")));
        row.put("dataType", readMapString(property, "dataType"));
        row.put("unit", Strings.sBlank(readMapString(property, "unit"), readMapString(log, "unit")));
        row.put("description", readMapString(property, "description"));
        row.put("valueJson", readMapString(log, "valueJson"));
        row.put("deviceAt", readMapLong(log, "deviceAt"));
        row.put("createdAt", readMapLong(log, "createdAt"));
        return row;
    }

    private List<Map<String, Object>> parseThingPropertyList(String raw) {
        if (Strings.isBlank(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> toRawLogView(Device_raw_log log, Device_product product, String tenantId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", log.getId());
        row.put("direction", log.getDirection());
        row.put("messageType", log.getMessageType());
        row.put("protocol", log.getProtocol());
        row.put("topic", log.getTopic());
        row.put("gatewayNodeId", log.getGatewayNodeId());
        row.put("payload", log.getPayload());
        row.put("success", log.isSuccess());
        row.put("deviceAt", log.getDeviceAt());
        row.put("createdAt", log.getCreatedAt());
        row.put("parsedJson", Strings.sBlank(log.getParsedJson(), ""));
        return row;
    }

    private String readMapString(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key) || source.get(key) == null) {
            return "";
        }
        return Strings.sNull(String.valueOf(source.get(key))).trim();
    }

    private Long readMapLong(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key) || source.get(key) == null) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

}

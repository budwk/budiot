package com.budwk.sp.device.services.impl;

import com.budwk.sp.device.dto.DeviceRuleConditionDTO;
import com.budwk.sp.device.dto.DeviceRuleDTO;
import com.budwk.sp.device.dto.DeviceRuleLinkageParamDTO;
import com.budwk.sp.device.entity.Device_info;
import com.budwk.sp.device.entity.Device_product;
import com.budwk.sp.device.entity.Device_rule;
import com.budwk.sp.device.enums.DeviceRuleTargetType;
import com.budwk.sp.device.enums.DeviceRuleTriggerScene;
import com.budwk.sp.device.services.DeviceInfoService;
import com.budwk.sp.device.services.DeviceProductService;
import com.budwk.sp.device.services.DeviceRuleService;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.database.service.BaseServiceImpl;
import com.budwk.sp.sys.entity.Sys_user;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceRuleServiceImpl extends BaseServiceImpl<Device_rule> implements DeviceRuleService {
    private final DeviceProductService deviceProductService;
    private final DeviceInfoService deviceInfoService;
    private final ObjectMapper objectMapper;

    public DeviceRuleServiceImpl(Dao dao,
                                 DeviceProductService deviceProductService,
                                 DeviceInfoService deviceInfoService,
                                 ObjectMapper objectMapper) {
        super(dao);
        this.deviceProductService = deviceProductService;
        this.deviceInfoService = deviceInfoService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Device_rule createRule(DeviceRuleDTO dto, String operatorId, String tenantId) {
        checkCodeUnique(null, tenantId, dto.getCode());
        Device_rule rule = build(dto, new Device_rule(), operatorId, tenantId);
        rule.setCreatedBy(operatorId);
        this.insert(rule);
        return rule;
    }

    @Override
    public Device_rule updateRule(DeviceRuleDTO dto, String operatorId, String tenantId) {
        Device_rule rule = getRule(dto.getId(), tenantId);
        checkCodeUnique(rule.getId(), tenantId, dto.getCode());
        build(dto, rule, operatorId, tenantId);
        this.updateIgnoreNull(rule);
        return rule;
    }

    @Override
    public void deleteRule(String id, String tenantId) {
        this.delete(getRule(id, tenantId).getId());
    }

    @Override
    public Device_rule getRule(String id, String tenantId) {
        Device_rule rule = this.fetch(Cnd.where("id", "=", id).and("tenantId", "=", tenantId).and("delFlag", "=", false));
        if (rule == null) {
            throw new BaseException("设备规则不存在");
        }
        return rule;
    }

    @Override
    public Map<String, Object> getBaseData(String tenantId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("triggerScenes", Arrays.stream(DeviceRuleTriggerScene.values()).map(item -> option(item.getText(), item.getValue())).toList());
        map.put("targetTypes", Arrays.stream(DeviceRuleTargetType.values()).map(item -> option(item.getText(), item.getValue())).toList());
        map.put("products", deviceProductService.listEnabled(tenantId).stream().map(this::toProductOption).toList());
        map.put("tenantUsers", dao().query(Sys_user.class, Cnd.where("tenantId", "=", tenantId)
                .and("disabled", "=", false)
                .and("delFlag", "=", false)
                .asc("createdAt")).stream().map(this::toUserOption).toList());
        map.put("smsChannels", dao().query(Msg_channel.class, Cnd.where("tenantId", "=", tenantId)
                .and("channelType", "=", MsgChannelType.SMS)
                .and("disabled", "=", false)
                .and("delFlag", "=", false)
                .asc("createdAt")).stream().map(this::toChannelOption).toList());
        return map;
    }

    @Override
    public Map<String, Object> getProductMeta(String productId, String tenantId) {
        Device_product product = deviceProductService.getProduct(productId, tenantId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("product", toProductOption(product));
        map.put("devices", dao().query(Device_info.class, Cnd.where("tenantId", "=", tenantId)
                .and("productId", "=", productId)
                .and("delFlag", "=", false)
                .asc("createdAt")).stream().map(this::toDeviceOption).toList());
        map.put("propertyFields", parsePropertyFields(product.getThingPropertyJson()));
        map.put("events", parseEvents(product.getThingEventJson()));
        map.put("services", parseServices(product.getThingServiceJson()));
        return map;
    }

    private Device_rule build(DeviceRuleDTO dto, Device_rule rule, String operatorId, String tenantId) {
        Device_product sourceProduct = deviceProductService.getProduct(dto.getSourceProductId(), tenantId);
        Device_info sourceDevice = null;
        if (Strings.isNotBlank(dto.getSourceDeviceId())) {
            sourceDevice = deviceInfoService.getDevice(dto.getSourceDeviceId(), tenantId);
            if (!sourceProduct.getId().equals(sourceDevice.getProductId())) {
                throw new BaseException("来源设备与来源产品不匹配");
            }
        }
        List<DeviceRuleConditionDTO> conditions = normalizeConditions(dto.getConditionJson());
        List<String> notifyUserIds = normalizeNotifyUserIds(dto.getNotifyUserIdsJson(), tenantId);
        List<DeviceRuleLinkageParamDTO> linkageParams = normalizeLinkageParams(dto.getLinkageParamsJson());
        validate(dto, tenantId, sourceProduct, conditions, linkageParams);

        rule.setTenantId(tenantId);
        rule.setName(Strings.sNull(dto.getName()).trim());
        rule.setCode(Strings.sNull(dto.getCode()).trim());
        rule.setTriggerScene(dto.getTriggerScene());
        rule.setSourceProductId(sourceProduct.getId());
        rule.setSourceDeviceId(sourceDevice == null ? "" : sourceDevice.getId());
        rule.setTriggerIdentifier(Strings.sNull(dto.getTriggerIdentifier()).trim());
        rule.setConditionJson(writeJson(conditions));
        rule.setTargetType(dto.getTargetType());
        rule.setActionTitle(Strings.sNull(dto.getActionTitle()).trim());
        rule.setActionContent(Strings.sNull(dto.getActionContent()).trim());
        rule.setMessageChannelId(Strings.sNull(dto.getMessageChannelId()).trim());
        rule.setNotifyUserIdsJson(writeJson(notifyUserIds));
        rule.setTargetUrl(Strings.sNull(dto.getTargetUrl()).trim());
        rule.setTargetTopic(Strings.sNull(dto.getTargetTopic()).trim());
        rule.setLinkageProductId(Strings.sNull(dto.getLinkageProductId()).trim());
        rule.setLinkageDeviceId(Strings.sNull(dto.getLinkageDeviceId()).trim());
        rule.setLinkageServiceIdentifier(Strings.sNull(dto.getLinkageServiceIdentifier()).trim());
        rule.setLinkageParamsJson(writeJson(linkageParams));
        rule.setDescription(Strings.sNull(dto.getDescription()).trim());
        rule.setDisabled(dto.isDisabled());
        rule.setUpdatedBy(operatorId);
        return rule;
    }

    private void validate(DeviceRuleDTO dto,
                          String tenantId,
                          Device_product sourceProduct,
                          List<DeviceRuleConditionDTO> conditions,
                          List<DeviceRuleLinkageParamDTO> linkageParams) {
        if (dto.getTriggerScene() == DeviceRuleTriggerScene.EVENT && Strings.isBlank(dto.getTriggerIdentifier())) {
            throw new BaseException("事件告警规则必须选择触发事件");
        }
        Set<String> validFields = dto.getTriggerScene() == DeviceRuleTriggerScene.NORMALIZED_UPLINK
                ? parsePropertyFields(sourceProduct.getThingPropertyJson()).stream().map(item -> Strings.sNull((String) item.get("value")).trim()).collect(Collectors.toSet())
                : parseEvents(sourceProduct.getThingEventJson()).stream()
                .filter(item -> Strings.sNull((String) item.get("identifier")).trim().equals(Strings.sNull(dto.getTriggerIdentifier()).trim()))
                .flatMap(item -> ((List<Map<String, Object>>) item.getOrDefault("outputParams", List.of())).stream())
                .map(item -> Strings.sNull((String) item.get("identifier")).trim())
                .collect(Collectors.toSet());
        for (DeviceRuleConditionDTO condition : conditions) {
            if (!validFields.contains(Strings.sNull(condition.getField()).trim())) {
                throw new BaseException("规则条件字段不存在: " + condition.getField());
            }
        }
        switch (dto.getTargetType()) {
            case SMS -> validateSms(dto, tenantId);
            case SITE_MESSAGE -> validateSiteMessage(dto);
            case HTTP_PUSH -> validateHttpPush(dto);
            case QUEUE_PUSH -> validateQueuePush(dto);
            case SCENE_LINKAGE -> validateSceneLinkage(dto, tenantId, linkageParams);
        }
    }

    private void validateSms(DeviceRuleDTO dto, String tenantId) {
        if (Strings.isBlank(dto.getActionContent())) {
            throw new BaseException("短信内容不能为空");
        }
        if (Strings.isNotBlank(dto.getMessageChannelId())) {
            Msg_channel channel = dao().fetch(Msg_channel.class, Cnd.where("id", "=", dto.getMessageChannelId())
                    .and("tenantId", "=", tenantId)
                    .and("delFlag", "=", false));
            if (channel == null || channel.getChannelType() != MsgChannelType.SMS || channel.isDisabled()) {
                throw new BaseException("短信渠道不存在或不可用");
            }
        }
    }

    private void validateSiteMessage(DeviceRuleDTO dto) {
        if (Strings.isBlank(dto.getActionTitle())) {
            throw new BaseException("站内信标题不能为空");
        }
        if (Strings.isBlank(dto.getActionContent())) {
            throw new BaseException("站内信内容不能为空");
        }
    }

    private void validateHttpPush(DeviceRuleDTO dto) {
        if (Strings.isBlank(dto.getTargetUrl())) {
            throw new BaseException("HTTP推送必须填写目标地址");
        }
    }

    private void validateQueuePush(DeviceRuleDTO dto) {
        if (Strings.isBlank(dto.getTargetTopic())) {
            throw new BaseException("队列推送必须填写目标主题");
        }
    }

    private void validateSceneLinkage(DeviceRuleDTO dto, String tenantId, List<DeviceRuleLinkageParamDTO> linkageParams) {
        if (Strings.isBlank(dto.getLinkageProductId()) || Strings.isBlank(dto.getLinkageDeviceId()) || Strings.isBlank(dto.getLinkageServiceIdentifier())) {
            throw new BaseException("场景联动必须选择目标产品、目标设备和目标服务");
        }
        Device_product targetProduct = deviceProductService.getProduct(dto.getLinkageProductId(), tenantId);
        Device_info targetDevice = deviceInfoService.getDevice(dto.getLinkageDeviceId(), tenantId);
        if (!targetProduct.getId().equals(targetDevice.getProductId())) {
            throw new BaseException("联动设备与联动产品不匹配");
        }
        Map<String, Object> targetService = parseServices(targetProduct.getThingServiceJson()).stream()
                .filter(item -> Strings.sNull((String) item.get("identifier")).trim().equals(Strings.sNull(dto.getLinkageServiceIdentifier()).trim()))
                .findFirst()
                .orElseThrow(() -> new BaseException("联动服务不存在"));
        Set<String> validParams = ((List<Map<String, Object>>) targetService.getOrDefault("inputParams", List.of())).stream()
                .map(item -> Strings.sNull((String) item.get("identifier")).trim())
                .collect(Collectors.toSet());
        for (DeviceRuleLinkageParamDTO param : linkageParams) {
            if (!validParams.contains(Strings.sNull(param.getIdentifier()).trim())) {
                throw new BaseException("联动参数不存在: " + param.getIdentifier());
            }
        }
    }

    private List<DeviceRuleConditionDTO> normalizeConditions(String conditionJson) {
        List<DeviceRuleConditionDTO> list = readList(conditionJson, new TypeReference<>() {});
        List<DeviceRuleConditionDTO> result = new ArrayList<>();
        for (DeviceRuleConditionDTO item : list) {
            if (item == null || Strings.isBlank(item.getField()) || Strings.isBlank(item.getOperator())) {
                continue;
            }
            DeviceRuleConditionDTO dto = new DeviceRuleConditionDTO();
            dto.setField(Strings.sNull(item.getField()).trim());
            dto.setFieldName(Strings.sNull(item.getFieldName()).trim());
            dto.setDataType(Strings.sNull(item.getDataType()).trim());
            dto.setOperator(Strings.sNull(item.getOperator()).trim().toUpperCase(Locale.ROOT));
            dto.setValue(Strings.sNull(item.getValue()).trim());
            result.add(dto);
        }
        return result;
    }

    private List<String> normalizeNotifyUserIds(String notifyUserIdsJson, String tenantId) {
        List<String> raw = readList(notifyUserIdsJson, new TypeReference<>() {});
        if (raw.isEmpty()) {
            return List.of();
        }
        Set<String> validUserIds = dao().query(Sys_user.class, Cnd.where("tenantId", "=", tenantId)
                .and("delFlag", "=", false)).stream().map(Sys_user::getId).collect(Collectors.toSet());
        List<String> result = new ArrayList<>();
        for (String item : raw) {
            String userId = Strings.sNull(item).trim();
            if (Strings.isBlank(userId)) {
                continue;
            }
            if (!validUserIds.contains(userId)) {
                throw new BaseException("通知用户不存在: " + userId);
            }
            if (!result.contains(userId)) {
                result.add(userId);
            }
        }
        return result;
    }

    private List<DeviceRuleLinkageParamDTO> normalizeLinkageParams(String linkageParamsJson) {
        List<DeviceRuleLinkageParamDTO> list = readList(linkageParamsJson, new TypeReference<>() {});
        List<DeviceRuleLinkageParamDTO> result = new ArrayList<>();
        for (DeviceRuleLinkageParamDTO item : list) {
            if (item == null || Strings.isBlank(item.getIdentifier())) {
                continue;
            }
            DeviceRuleLinkageParamDTO dto = new DeviceRuleLinkageParamDTO();
            dto.setIdentifier(Strings.sNull(item.getIdentifier()).trim());
            dto.setName(Strings.sNull(item.getName()).trim());
            dto.setDataType(Strings.sNull(item.getDataType()).trim());
            dto.setValue(Strings.sNull(item.getValue()).trim());
            result.add(dto);
        }
        return result;
    }

    private Map<String, Object> toProductOption(Device_product item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("productKey", item.getProductKey());
        map.put("text", item.getName() + " (" + item.getProductKey() + ")");
        return map;
    }

    private Map<String, Object> toUserOption(Sys_user item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("username", item.getUsername());
        map.put("loginname", item.getLoginname());
        map.put("name", item.getUsername());
        map.put("mobile", item.getMobile());
        map.put("email", item.getEmail());
        map.put("text", Strings.sBlank(item.getUsername(), item.getLoginname()));
        return map;
    }

    private Map<String, Object> toChannelOption(Msg_channel item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("code", item.getCode());
        map.put("text", item.getName() + " (" + item.getCode() + ")");
        map.put("defaultFlag", item.isDefaultFlag());
        return map;
    }

    private Map<String, Object> toDeviceOption(Device_info item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("deviceCode", item.getDeviceCode());
        map.put("name", item.getName());
        map.put("text", Strings.sBlank(item.getName(), item.getDeviceCode()) + " (" + item.getDeviceCode() + ")");
        return map;
    }

    private Map<String, Object> option(String text, String value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", text);
        map.put("value", value);
        return map;
    }

    private List<Map<String, Object>> parsePropertyFields(String raw) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : readList(raw, new TypeReference<List<Map<String, Object>>>() {})) {
            String identifier = stringValue(item.get("identifier"));
            if (Strings.isBlank(identifier)) {
                continue;
            }
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("identifier", identifier);
            field.put("value", identifier);
            field.put("name", stringValue(item.get("name")));
            field.put("text", Strings.sBlank(stringValue(item.get("name")), identifier));
            field.put("dataType", normalizeDataType(stringValue(item.get("dataType"))));
            list.add(field);
        }
        return list;
    }

    private List<Map<String, Object>> parseEvents(String raw) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : readList(raw, new TypeReference<List<Map<String, Object>>>() {})) {
            String identifier = stringValue(item.get("identifier"));
            if (Strings.isBlank(identifier)) {
                continue;
            }
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("identifier", identifier);
            event.put("name", stringValue(item.get("name")));
            event.put("text", Strings.sBlank(stringValue(item.get("name")), identifier));
            event.put("level", stringValue(item.get("level")));
            List<Map<String, Object>> outputParams = new ArrayList<>();
            for (Map<String, Object> param : readList(item.get("outputParams"), new TypeReference<List<Map<String, Object>>>() {})) {
                String paramIdentifier = stringValue(param.get("identifier"));
                if (Strings.isBlank(paramIdentifier)) {
                    continue;
                }
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("identifier", paramIdentifier);
                field.put("name", stringValue(param.get("name")));
                field.put("text", Strings.sBlank(stringValue(param.get("name")), paramIdentifier));
                field.put("dataType", normalizeDataType(stringValue(param.get("dataType"))));
                outputParams.add(field);
            }
            event.put("outputParams", outputParams);
            list.add(event);
        }
        return list;
    }

    private List<Map<String, Object>> parseServices(String raw) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : readList(raw, new TypeReference<List<Map<String, Object>>>() {})) {
            String identifier = stringValue(item.get("identifier"));
            if (Strings.isBlank(identifier)) {
                continue;
            }
            Map<String, Object> service = new LinkedHashMap<>();
            service.put("identifier", identifier);
            service.put("name", stringValue(item.get("name")));
            service.put("text", Strings.sBlank(stringValue(item.get("name")), identifier));
            service.put("callType", stringValue(item.get("callType")));
            service.put("description", stringValue(item.get("description")));
            List<Map<String, Object>> inputParams = new ArrayList<>();
            for (Map<String, Object> param : readList(item.get("inputParams"), new TypeReference<List<Map<String, Object>>>() {})) {
                String paramIdentifier = stringValue(param.get("identifier"));
                if (Strings.isBlank(paramIdentifier)) {
                    continue;
                }
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("identifier", paramIdentifier);
                field.put("name", stringValue(param.get("name")));
                field.put("text", Strings.sBlank(stringValue(param.get("name")), paramIdentifier));
                field.put("dataType", normalizeDataType(stringValue(param.get("dataType"))));
                field.put("required", Boolean.TRUE.equals(param.get("required")));
                inputParams.add(field);
            }
            service.put("inputParams", inputParams);
            list.add(service);
        }
        return list;
    }

    private String normalizeDataType(String raw) {
        String dataType = Strings.sNull(raw).trim().toLowerCase(Locale.ROOT);
        if (Set.of("int", "integer", "long", "double", "float", "number", "decimal").contains(dataType)) {
            return "number";
        }
        if (Set.of("bool", "boolean").contains(dataType)) {
            return "boolean";
        }
        return Strings.sBlank(dataType, "string");
    }

    private String stringValue(Object value) {
        return value == null ? "" : Strings.sNull(String.valueOf(value)).trim();
    }

    private <T> List<T> readList(Object raw, TypeReference<List<T>> typeReference) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return objectMapper.convertValue(list, typeReference);
        }
        String json = Strings.sNull(String.valueOf(raw)).trim();
        if (Strings.isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new BaseException("JSON数据格式不正确: " + e.getMessage());
        }
    }

    private String writeJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data == null ? List.of() : data);
        } catch (Exception e) {
            throw new IllegalStateException("serialize rule config error", e);
        }
    }

    private void checkCodeUnique(String id, String tenantId, String code) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("code", "=", Strings.sNull(code).trim()).and("delFlag", "=", false);
        if (Strings.isNotBlank(id)) {
            cnd.and("id", "<>", id);
        }
        if (this.count(cnd) > 0) {
            throw new BaseException("规则编码已存在");
        }
    }
}

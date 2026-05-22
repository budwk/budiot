package com.budwk.sp.device.rule.service;

import com.budwk.sp.device.dto.*;
import com.budwk.sp.device.entity.Device_rule;
import com.budwk.sp.device.enums.DeviceMessagePattern;
import com.budwk.sp.device.enums.DeviceMessageScene;
import com.budwk.sp.device.enums.DeviceRuleTargetType;
import com.budwk.sp.device.enums.DeviceRuleTriggerScene;
import com.budwk.sp.device.message.DeviceMessageTemplate;
import com.budwk.sp.device.providers.IDeviceCommandProvider;
import com.budwk.sp.device.rule.config.DeviceRuleProperties;
import com.budwk.sp.msg.dto.MsgSendDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.providers.IMsgChannelProvider;
import com.budwk.sp.msg.providers.IMsgSendProvider;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.enums.SysMsgType;
import com.budwk.sp.sys.providers.ISysMsgProvider;
import com.budwk.sp.sys.providers.ISysUserProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class DeviceRuleEngineService {
    private final Dao dao;
    private final ObjectMapper objectMapper;
    private final DeviceMessageTemplate deviceMessageTemplate;
    private final DeviceRuleProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @DubboReference(interfaceClass = IMsgSendProvider.class, check = false, lazy = true, retries = 0)
    private IMsgSendProvider msgSendProvider;
    @DubboReference(interfaceClass = IMsgChannelProvider.class, check = false, lazy = true, retries = 0)
    private IMsgChannelProvider msgChannelProvider;
    @DubboReference(interfaceClass = ISysMsgProvider.class, check = false, lazy = true, retries = 0)
    private ISysMsgProvider sysMsgProvider;
    @DubboReference(interfaceClass = ISysUserProvider.class, check = false, lazy = true, retries = 0)
    private ISysUserProvider sysUserProvider;
    @DubboReference(interfaceClass = IDeviceCommandProvider.class, check = false, lazy = true, retries = 0)
    private IDeviceCommandProvider deviceCommandProvider;

    public DeviceRuleEngineService(Dao dao, ObjectMapper objectMapper, DeviceMessageTemplate deviceMessageTemplate, DeviceRuleProperties properties) {
        this.dao = dao;
        this.objectMapper = objectMapper;
        this.deviceMessageTemplate = deviceMessageTemplate;
        this.properties = properties;
    }

    public void handleNormalized(DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope) {
        List<Device_rule> rules = loadRules(envelope.getTenantId(), envelope.getProductId(), DeviceRuleTriggerScene.NORMALIZED_UPLINK);
        for (Device_rule rule : rules) {
            if (!matchNormalized(rule, envelope)) {
                continue;
            }
            execute(rule, envelope, buildNormalizedContext(envelope), buildNormalizedContent(envelope), payloadIdentifier(envelope.getPayload()));
        }
    }

    public void handleEvent(DeviceMessageEnvelope<DeviceEventMessageDTO> envelope) {
        List<Device_rule> rules = loadRules(envelope.getTenantId(), envelope.getProductId(), DeviceRuleTriggerScene.EVENT);
        for (Device_rule rule : rules) {
            if (!matchEvent(rule, envelope)) {
                continue;
            }
            execute(rule, envelope, buildEventContext(envelope), buildEventContent(envelope), envelope.getPayload() == null ? "" : envelope.getPayload().getEventCode());
        }
    }

    private List<Device_rule> loadRules(String tenantId, String productId, DeviceRuleTriggerScene scene) {
        Cnd cnd = Cnd.where("tenantId", "=", tenantId).and("sourceProductId", "=", productId).and("triggerScene", "=", scene).and("disabled", "=", false).and("delFlag", "=", false);
        cnd.asc("createdAt");
        return dao.query(Device_rule.class, cnd);
    }

    private boolean matchNormalized(Device_rule rule, DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope) {
        DeviceUplinkMessageDTO payload = envelope.getPayload();
        if (payload == null) {
            return false;
        }
        if (Strings.isNotBlank(rule.getSourceDeviceId()) && !rule.getSourceDeviceId().equals(envelope.getDeviceId())) {
            return false;
        }
        if (Strings.isNotBlank(rule.getTriggerIdentifier()) && !rule.getTriggerIdentifier().equalsIgnoreCase(payloadIdentifier(payload))) {
            return false;
        }
        return matchConditions(rule.getConditionJson(), mergeJson(payload.getDataJson(), payload.getMetadataJson()));
    }

    private boolean matchEvent(Device_rule rule, DeviceMessageEnvelope<DeviceEventMessageDTO> envelope) {
        DeviceEventMessageDTO payload = envelope.getPayload();
        if (payload == null) {
            return false;
        }
        if (Strings.isNotBlank(rule.getSourceDeviceId()) && !rule.getSourceDeviceId().equals(envelope.getDeviceId())) {
            return false;
        }
        if (Strings.isNotBlank(rule.getTriggerIdentifier()) && !rule.getTriggerIdentifier().equalsIgnoreCase(Strings.sNull(payload.getEventCode()).trim())) {
            return false;
        }
        return matchConditions(rule.getConditionJson(), mergeJson(payload.getContentJson(), null));
    }

    private boolean matchConditions(String conditionJson, Map<String, Object> values) {
        List<DeviceRuleConditionDTO> conditions = readList(conditionJson, new TypeReference<>() {
        });
        if (conditions.isEmpty()) {
            return true;
        }
        for (DeviceRuleConditionDTO condition : conditions) {
            String field = Strings.sNull(condition.getField()).trim();
            if (Strings.isBlank(field)) {
                continue;
            }
            if (!matchCondition(condition, values.get(field))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchCondition(DeviceRuleConditionDTO condition, Object actualValue) {
        String operator = Strings.sNull(condition.getOperator()).trim().toUpperCase(Locale.ROOT);
        String expected = Strings.sNull(condition.getValue()).trim();
        String dataType = Strings.sBlank(condition.getDataType(), "string").trim().toLowerCase(Locale.ROOT);
        if ("number".equals(dataType)) {
            Double actual = toDouble(actualValue);
            Double target = toDouble(expected);
            if (actual == null || target == null) {
                return false;
            }
            return switch (operator) {
                case "GT" -> actual > target;
                case "LT" -> actual < target;
                default -> Double.compare(actual, target) == 0;
            };
        }
        if ("boolean".equals(dataType)) {
            return Boolean.parseBoolean(String.valueOf(actualValue)) == Boolean.parseBoolean(expected);
        }
        return String.valueOf(actualValue == null ? "" : actualValue).equals(expected);
    }

    private void execute(Device_rule rule, DeviceMessageEnvelope<?> sourceEnvelope, Map<String, Object> context, String defaultContentJson, String sourceIdentifier) {
        try {
            switch (rule.getTargetType()) {
                case SMS -> executeSms(rule, sourceEnvelope.getTenantId(), context);
                case SITE_MESSAGE -> executeSiteMessage(rule, sourceEnvelope.getTenantId(), context, false);
                case HTTP_PUSH -> executeHttpPush(rule, context, defaultContentJson);
                case QUEUE_PUSH ->
                        executeQueuePush(rule, sourceEnvelope, context, defaultContentJson, sourceIdentifier);
                case SCENE_LINKAGE -> executeSceneLinkage(rule, sourceEnvelope.getTenantId(), context);
            }
        } catch (Exception e) {
            log.error("execute device rule [{}] failed", rule.getCode(), e);
        }
    }

    private void executeSms(Device_rule rule, String tenantId, Map<String, Object> context) {
        Msg_channel channel = Strings.isNotBlank(rule.getMessageChannelId()) ? msgChannelProvider.getEnabledChannels(tenantId).stream().filter(item -> item.getId().equals(rule.getMessageChannelId())).findFirst().orElse(null) : msgChannelProvider.getDefaultChannel(tenantId, MsgChannelType.SMS.name());
        if (channel == null) {
            throw new IllegalStateException("未配置可用短信渠道");
        }
        List<String> receivers = resolveNotifyUserIds(rule, tenantId);
        if (receivers.isEmpty()) {
            log.warn("rule [{}] has no SMS receivers", rule.getCode());
            return;
        }
        MsgSendDTO dto = new MsgSendDTO();
        dto.setTenantId(tenantId);
        dto.setChannelId(channel.getId());
        dto.setTitle(render(rule.getActionTitle(), context));
        dto.setContent(render(rule.getActionContent(), context));
        dto.setUserIds(receivers.toArray(new String[0]));
        msgSendProvider.send(dto, resolveOperatorId(rule), tenantId);
    }

    private void executeSiteMessage(Device_rule rule, String tenantId, Map<String, Object> context, boolean linkageNotice) {
        List<String> userIds = resolveNotifyUserIds(rule, tenantId);
        if (userIds.isEmpty()) {
            log.warn("rule [{}] has no site-message receivers", rule.getCode());
            return;
        }
        String title = render(linkageNotice && Strings.isBlank(rule.getActionTitle()) ? defaultLinkageTitle(rule, context) : rule.getActionTitle(), context);
        String content = render(linkageNotice && Strings.isBlank(rule.getActionContent()) ? defaultLinkageContent(rule, context) : rule.getActionContent(), context);
        sysMsgProvider.sendMsg(userIds.toArray(new String[0]), SysMsgType.SYSTEM, title, "/platform/iot/rule", content, resolveOperatorId(rule));
    }

    private void executeHttpPush(Device_rule rule, Map<String, Object> context, String defaultContentJson) throws Exception {
        String body = Strings.isBlank(rule.getActionContent()) ? defaultContentJson : render(rule.getActionContent(), context);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(rule.getTargetUrl())).timeout(Duration.ofMillis(Math.max(properties.getWebhookTimeoutMs(), 1000L))).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("http push response status=" + response.statusCode());
        }
    }

    private void executeQueuePush(Device_rule rule, DeviceMessageEnvelope<?> sourceEnvelope, Map<String, Object> context, String defaultContentJson, String sourceIdentifier) {
        String contentJson = Strings.isBlank(rule.getActionContent()) ? defaultContentJson : render(rule.getActionContent(), context);
        DeviceRuleForwardMessageDTO payload = new DeviceRuleForwardMessageDTO();
        payload.setTenantId(sourceEnvelope.getTenantId());
        payload.setProductId(sourceEnvelope.getProductId());
        payload.setProductKey(sourceEnvelope.getProductKey());
        payload.setDeviceId(sourceEnvelope.getDeviceId());
        payload.setDeviceCode(sourceEnvelope.getDeviceCode());
        payload.setSourceScene(sourceEnvelope.getScene() == null ? "" : sourceEnvelope.getScene().getValue());
        payload.setSourceIdentifier(Strings.sNull(sourceIdentifier).trim());
        payload.setRuleCode(rule.getCode());
        payload.setRuleName(rule.getName());
        payload.setTargetType(DeviceRuleTargetType.QUEUE_PUSH.getValue());
        payload.setTargetTopic(Strings.sNull(rule.getTargetTopic()).trim());
        payload.setContentJson(contentJson);
        payload.setTriggerAt(System.currentTimeMillis());

        DeviceMessageEnvelope<DeviceRuleForwardMessageDTO> envelope = new DeviceMessageEnvelope<>();
        envelope.setTenantId(sourceEnvelope.getTenantId());
        envelope.setProductId(sourceEnvelope.getProductId());
        envelope.setProductKey(sourceEnvelope.getProductKey());
        envelope.setDeviceId(sourceEnvelope.getDeviceId());
        envelope.setDeviceCode(sourceEnvelope.getDeviceCode());
        envelope.setGatewayNodeId(sourceEnvelope.getGatewayNodeId());
        envelope.setScene(DeviceMessageScene.RULE_FORWARD);
        envelope.setPattern(DeviceMessagePattern.TOPIC);
        envelope.setOccurredAt(System.currentTimeMillis());
        envelope.setRoutingKey(Strings.sBlank(sourceEnvelope.getDeviceCode(), rule.getCode()));
        envelope.setTopic(Strings.sNull(rule.getTargetTopic()).trim());
        envelope.getHeaders().put("ruleCode", rule.getCode());
        envelope.getHeaders().put("targetType", DeviceRuleTargetType.QUEUE_PUSH.getValue());
        envelope.setPayload(payload);
        deviceMessageTemplate.publish(envelope, rule.getCode(), "rule");
    }

    private void executeSceneLinkage(Device_rule rule, String tenantId, Map<String, Object> context) {
        DeviceCommandDTO dto = new DeviceCommandDTO();
        dto.setDeviceId(rule.getLinkageDeviceId());
        dto.setCommandCode(rule.getLinkageServiceIdentifier());
        dto.setPayloadJson(renderLinkageParams(rule, context));
        dto.setReplyRequired(false);
        deviceCommandProvider.createCommands(dto, resolveOperatorId(rule), tenantId);
        executeSiteMessage(rule, tenantId, context, true);
    }

    private String renderLinkageParams(Device_rule rule, Map<String, Object> context) {
        List<DeviceRuleLinkageParamDTO> params = readList(rule.getLinkageParamsJson(), new TypeReference<>() {
        });
        Map<String, Object> map = new LinkedHashMap<>();
        for (DeviceRuleLinkageParamDTO item : params) {
            if (item == null || Strings.isBlank(item.getIdentifier())) {
                continue;
            }
            map.put(item.getIdentifier(), parseTypedValue(item.getDataType(), render(item.getValue(), context)));
        }
        return writeJson(map);
    }

    private List<String> resolveNotifyUserIds(Device_rule rule, String tenantId) {
        List<String> configured = readList(rule.getNotifyUserIdsJson(), new TypeReference<>() {
        });
        if (!configured.isEmpty()) {
            return configured.stream().filter(Strings::isNotBlank).distinct().toList();
        }
        List<Sys_user> users = sysUserProvider.listEnabledByTenantId(tenantId);
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream().map(Sys_user::getId).filter(Strings::isNotBlank).distinct().toList();
    }

    private Map<String, Object> buildNormalizedContext(DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope) {
        Map<String, Object> context = createBaseContext(envelope);
        DeviceUplinkMessageDTO payload = envelope.getPayload();
        if (payload != null) {
            context.put("identifier", payloadIdentifier(payload));
            context.put("messageType", Strings.sNull(payload.getMessageType()).trim());
            context.putAll(mergeJson(payload.getDataJson(), payload.getMetadataJson()));
        }
        return context;
    }

    private Map<String, Object> buildEventContext(DeviceMessageEnvelope<DeviceEventMessageDTO> envelope) {
        Map<String, Object> context = createBaseContext(envelope);
        DeviceEventMessageDTO payload = envelope.getPayload();
        if (payload != null) {
            context.put("eventCode", Strings.sNull(payload.getEventCode()).trim());
            context.put("eventLevel", Strings.sNull(payload.getLevel()).trim());
            context.putAll(mergeJson(payload.getContentJson(), null));
        }
        return context;
    }

    private Map<String, Object> createBaseContext(DeviceMessageEnvelope<?> envelope) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("tenantId", envelope.getTenantId());
        context.put("productId", envelope.getProductId());
        context.put("productKey", envelope.getProductKey());
        context.put("deviceId", envelope.getDeviceId());
        context.put("deviceCode", envelope.getDeviceCode());
        context.put("gatewayNodeId", envelope.getGatewayNodeId());
        context.put("scene", envelope.getScene() == null ? "" : envelope.getScene().getValue());
        context.put("occurredAt", envelope.getOccurredAt());
        return context;
    }

    private String buildNormalizedContent(DeviceMessageEnvelope<DeviceUplinkMessageDTO> envelope) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(createBaseContext(envelope));
        map.put("payload", envelope.getPayload());
        return writeJson(map);
    }

    private String buildEventContent(DeviceMessageEnvelope<DeviceEventMessageDTO> envelope) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(createBaseContext(envelope));
        map.put("payload", envelope.getPayload());
        return writeJson(map);
    }

    private String defaultLinkageTitle(Device_rule rule, Map<String, Object> context) {
        return "规则[" + rule.getName() + "]已触发";
    }

    private String defaultLinkageContent(Device_rule rule, Map<String, Object> context) {
        return "设备${deviceCode}触发规则，已执行联动服务 " + Strings.sNull(rule.getLinkageServiceIdentifier()).trim();
    }

    private String resolveOperatorId(Device_rule rule) {
        return Strings.sBlank(rule.getUpdatedBy(), Strings.sBlank(rule.getCreatedBy(), "device-rule"));
    }

    private String payloadIdentifier(DeviceUplinkMessageDTO payload) {
        return Strings.sBlank(payload == null ? "" : payload.getIdentifier(), payload == null ? "" : payload.getMessageType()).trim();
    }

    private Map<String, Object> mergeJson(String firstJson, String secondJson) {
        Map<String, Object> map = new LinkedHashMap<>();
        putAllJson(map, firstJson);
        putAllJson(map, secondJson);
        return map;
    }

    private void putAllJson(Map<String, Object> map, String json) {
        if (Strings.isBlank(json)) {
            return;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {
            });
            map.putAll(parsed);
        } catch (Exception e) {
            map.put("raw_" + map.size(), json);
        }
    }

    private Double toDouble(Object value) {
        if (value == null || Strings.isBlank(String.valueOf(value))) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Object parseTypedValue(String dataType, String value) {
        String type = Strings.sBlank(dataType, "string").trim().toLowerCase(Locale.ROOT);
        if ("number".equals(type)) {
            Double number = toDouble(value);
            return number == null ? value : number;
        }
        if ("boolean".equals(type)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private String render(String template, Map<String, Object> params) {
        if (template == null) {
            return null;
        }
        String rendered = template;
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                rendered = rendered.replace("${" + entry.getKey() + "}", entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
            }
        }
        return rendered;
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> typeReference) {
        if (Strings.isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            log.warn("parse rule json failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String writeJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Map.of() : data);
        } catch (Exception e) {
            return "{}";
        }
    }
}

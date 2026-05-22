package com.budwk.sp.device.gateway.service;

import com.budwk.sp.device.dto.DeviceCommandStatusUpdateDTO;
import com.budwk.sp.device.dto.DeviceDownlinkCommandMessageDTO;
import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.enums.DeviceCommandStatus;
import com.budwk.sp.device.enums.DeviceNetworkProtocol;
import com.budwk.sp.device.gateway.support.GatewayCommandStatusService;
import com.budwk.sp.device.network.DeviceNetworkDownlinkMessage;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

/**
 * 网关下行指令服务
 * 
 * 改进点：
 * 1. 发送指令后更新状态为 SENT
 * 2. 记录发送日志
 * 3. 处理发送失败情况
 */
@Slf4j
@Service
public class GatewayDownlinkService {
    private final GatewaySessionService gatewaySessionService;
    private final GatewayNetworkManager gatewayNetworkManager;
    private final GatewayCommandStatusService gatewayCommandStatusService;
    private final Dao dao;

    public GatewayDownlinkService(GatewaySessionService gatewaySessionService,
                                  GatewayNetworkManager gatewayNetworkManager,
                                  GatewayCommandStatusService gatewayCommandStatusService,
                                  Dao dao) {
        this.gatewaySessionService = gatewaySessionService;
        this.gatewayNetworkManager = gatewayNetworkManager;
        this.gatewayCommandStatusService = gatewayCommandStatusService;
        this.dao = dao;
    }

    public void handle(DeviceMessageEnvelope<DeviceDownlinkCommandMessageDTO> envelope) {
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("下行指令消息为空");
            return;
        }
        
        DeviceDownlinkCommandMessageDTO payload = envelope.getPayload();
        String commandId = payload.getCommandId();
        boolean trackStatus = payload.isArchiveCommandHistory() || Strings.isNotBlank(commandId);
        String targetNode = gatewaySessionService.resolveBindingNode(
                payload.getGatewayNodeId(),
                payload.getTenantId(),
                payload.getDeviceId(),
                payload.getDeviceCode()
        );
        
        try {
            // 1. 查找设备绑定的网关节点
            if (Strings.isBlank(targetNode)) {
                log.warn("未找到设备绑定的网关节点: deviceId={}, deviceCode={}", 
                    payload.getDeviceId(), payload.getDeviceCode());
                updateCommandStatus(trackStatus, commandId, envelope.getMessageId(), DeviceCommandStatus.FAILED, "", "未找到设备绑定的网关节点");
                return;
            }

            if (!gatewaySessionService.isLocalNode(targetNode)) {
                log.debug("下行消息不属于当前网关实例，忽略: commandId={}, targetNode={}", commandId, targetNode);
                return;
            }
            
            // 2. 检查设备是否在线
            if (!gatewaySessionService.isOnline(payload.getTenantId(), payload.getDeviceId())) {
                log.warn("设备离线，无法下发指令: deviceId={}, deviceCode={}", 
                    payload.getDeviceId(), payload.getDeviceCode());
                updateCommandStatus(trackStatus, commandId, envelope.getMessageId(), DeviceCommandStatus.FAILED, "", "设备离线");
                return;
            }
            
            // 3. 构建下行消息
            DeviceNetworkDownlinkMessage message = new DeviceNetworkDownlinkMessage();
            message.setBindingId(targetNode);
            message.setGatewayNodeId(targetNode);
            message.setDeviceId(payload.getDeviceId());
            message.setDeviceCode(payload.getDeviceCode());
            message.setProtocol(DeviceNetworkProtocol.valueOf(
                Strings.sBlank(payload.getNetworkProtocol(), "TCP")
            ));
            message.setTopic("");
            message.setPayload(Strings.sBlank(payload.getPayload(), payload.getPayloadJson()));
            
            // 4. 发送指令
            gatewayNetworkManager.send(message);
            
            // 5. 更新状态为 SENT
            updateCommandStatus(trackStatus, commandId, envelope.getMessageId(), DeviceCommandStatus.SENT, "", "");
            
            log.info("下行指令已发送: commandId={}, deviceCode={}, commandCode={}", 
                commandId, payload.getDeviceCode(), payload.getCommandCode());
            
        } catch (Exception e) {
            log.error("下行指令发送失败: commandId={}, error={}", commandId, e.getMessage(), e);
            updateCommandStatus(trackStatus, commandId, envelope.getMessageId(), DeviceCommandStatus.FAILED, "", "发送失败: " + e.getMessage());
        }
    }

    /**
     * 更新指令状态
     */
    private void updateCommandStatus(boolean trackStatus, String commandId, String messageId, DeviceCommandStatus status, String responseJson, String message) {
        if (!trackStatus || (Strings.isBlank(commandId) && Strings.isBlank(messageId))) {
            return;
        }
        
        try {
            DeviceCommandStatusUpdateDTO dto = new DeviceCommandStatusUpdateDTO();
            dto.setCommandId(commandId);
            dto.setMessageId(messageId);
            dto.setStatus(status);
            dto.setSentAt(status == DeviceCommandStatus.SENT ? System.currentTimeMillis() : null);
            dto.setResponseJson(responseJson);
            dto.setErrorMessage(message);
            dto.setFinishedAt(status == DeviceCommandStatus.SENT ? null : System.currentTimeMillis());
            dto.setUpdatedBy("gateway");
            gatewayCommandStatusService.updateStatus(dto);
        } catch (Exception e) {
            log.error("更新指令状态失败: commandId={}, messageId={}, error={}", commandId, messageId, e.getMessage());
        }
    }
}

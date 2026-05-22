package com.budwk.sp.device.handler.service;

import com.budwk.sp.device.dto.DeviceMessageEnvelope;
import com.budwk.sp.device.entity.Device_message_error;
import com.budwk.sp.device.enums.DeviceErrorMessageStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.lang.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 设备消息错误处理服务
 * 
 * 功能：
 * 1. 记录消费失败的消息到数据库
 * 2. 提供重试接口
 * 3. 支持死信队列管理
 */
@Slf4j
@Service
public class DeviceErrorMessageService {
    private final Dao dao;
    private final ObjectMapper objectMapper;
    private final DeviceRawMessageHandlerService handlerService;

    public DeviceErrorMessageService(Dao dao, 
                                     ObjectMapper objectMapper,
                                     DeviceRawMessageHandlerService handlerService) {
        this.dao = dao;
        this.objectMapper = objectMapper;
        this.handlerService = handlerService;
    }

    /**
     * 记录错误消息
     */
    public void recordError(DeviceMessageEnvelope<?> envelope, 
                           String rawMessage,
                           String errorType,
                           String errorMessage,
                           String stackTrace) {
        try {
            Device_message_error error = new Device_message_error();
            error.setTenantId(envelope.getTenantId());
            error.setProductId(envelope.getProductId());
            error.setProductKey(envelope.getProductKey());
            error.setDeviceId(envelope.getDeviceId());
            error.setDeviceCode(envelope.getDeviceCode());
            error.setScene(envelope.getScene() != null ? envelope.getScene().getValue() : "");
            error.setPattern(envelope.getPattern() != null ? envelope.getPattern().getValue() : "");
            error.setTopic(Strings.sBlank(envelope.getTopic(), ""));
            error.setMessageId(Strings.sBlank(envelope.getMessageId(), ""));
            error.setRoutingKey(Strings.sBlank(envelope.getRoutingKey(), ""));
            error.setOriginalTopic("");
            error.setRawMessage(rawMessage);
            error.setErrorType(errorType);
            error.setErrorMessage(truncate(errorMessage, 500));
            error.setStackTrace(truncate(stackTrace, 4000));
            error.setRetryCount(0);
            error.setMaxRetries(3);
            error.setStatus(DeviceErrorMessageStatus.PENDING.getValue());
            error.setOccurredAt(envelope.getOccurredAt());
            error.setCreatedAt(System.currentTimeMillis());
            error.setUpdatedAt(System.currentTimeMillis());
            error.setCreatedBy("system");
            error.setUpdatedBy("system");
            error.setDelFlag(false);
            
            dao.insert(error);
            
            log.warn("消息消费失败已记录: messageId={}, errorType={}, deviceCode={}", 
                error.getMessageId(), errorType, envelope.getDeviceCode());
                
        } catch (Exception e) {
            log.error("记录错误消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 重试错误消息
     */
    public boolean retry(String errorId) {
        Device_message_error error = dao.fetch(Device_message_error.class, errorId);
        if (error == null) {
            throw new IllegalArgumentException("错误消息不存在: " + errorId);
        }
        
        if (error.getRetryCount() >= error.getMaxRetries()) {
            error.setStatus(DeviceErrorMessageStatus.FAILED.getValue());
            error.setErrorMessage("已达到最大重试次数");
            error.setUpdatedAt(System.currentTimeMillis());
            dao.updateIgnoreNull(error);
            return false;
        }
        
        try {
            // 重新解析原始消息
            DeviceMessageEnvelope<?> envelope = objectMapper.readValue(
                error.getRawMessage(), 
                DeviceMessageEnvelope.class
            );
            
            // 调用处理服务
            if ("RAW_UPLINK".equals(error.getScene())) {
                handlerService.handle((DeviceMessageEnvelope) envelope);
            }
            
            // 更新状态为成功
            error.setStatus(DeviceErrorMessageStatus.SUCCESS.getValue());
            error.setRetryCount(error.getRetryCount() + 1);
            error.setUpdatedAt(System.currentTimeMillis());
            error.setUpdatedBy("retry");
            dao.updateIgnoreNull(error);
            
            log.info("消息重试成功: errorId={}, retryCount={}", errorId, error.getRetryCount());
            return true;
            
        } catch (Exception e) {
            // 更新重试次数和错误信息
            error.setRetryCount(error.getRetryCount() + 1);
            error.setErrorMessage(truncate(e.getMessage(), 500));
            error.setStackTrace(truncate(getStackTrace(e), 4000));
            error.setUpdatedAt(System.currentTimeMillis());
            error.setUpdatedBy("retry");
            
            if (error.getRetryCount() >= error.getMaxRetries()) {
                error.setStatus(DeviceErrorMessageStatus.FAILED.getValue());
            } else {
                error.setStatus(DeviceErrorMessageStatus.RETRYING.getValue());
            }
            
            dao.updateIgnoreNull(error);
            
            log.error("消息重试失败: errorId={}, retryCount={}", errorId, error.getRetryCount(), e);
            return false;
        }
    }

    /**
     * 批量重试
     */
    public int batchRetry(List<String> errorIds) {
        int successCount = 0;
        for (String errorId : errorIds) {
            try {
                if (retry(errorId)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量重试失败: errorId={}", errorId, e);
            }
        }
        return successCount;
    }

    /**
     * 查询待重试的消息
     */
    public List<Device_message_error> listPending(int limit) {
        return dao.query(Device_message_error.class, 
            Cnd.where("status", "=", DeviceErrorMessageStatus.PENDING.getValue())
                .and("delFlag", "=", false)
                .orderBy("createdAt", "ASC"), 
            new org.nutz.dao.pager.Pager(1, limit));
    }

    /**
     * 查询失败的消息
     */
    public List<Device_message_error> listFailed(int limit) {
        return dao.query(Device_message_error.class, 
            Cnd.where("status", "=", DeviceErrorMessageStatus.FAILED.getValue())
                .and("delFlag", "=", false)
                .orderBy("createdAt", "DESC"),
            new org.nutz.dao.pager.Pager(1, limit));
    }

    /**
     * 删除错误消息（软删除）
     */
    public void delete(String errorId) {
        Device_message_error error = dao.fetch(Device_message_error.class, errorId);
        if (error != null) {
            error.setDelFlag(true);
            error.setUpdatedAt(System.currentTimeMillis());
            dao.updateIgnoreNull(error);
        }
    }

    /**
     * 统计错误消息数量
     */
    public Map<String, Long> count() {
        long pending = dao.count(Device_message_error.class, 
            Cnd.where("status", "=", DeviceErrorMessageStatus.PENDING.getValue())
                .and("delFlag", "=", false));
        long retrying = dao.count(Device_message_error.class, 
            Cnd.where("status", "=", DeviceErrorMessageStatus.RETRYING.getValue())
                .and("delFlag", "=", false));
        long failed = dao.count(Device_message_error.class, 
            Cnd.where("status", "=", DeviceErrorMessageStatus.FAILED.getValue())
                .and("delFlag", "=", false));
        long success = dao.count(Device_message_error.class, 
            Cnd.where("status", "=", DeviceErrorMessageStatus.SUCCESS.getValue())
                .and("delFlag", "=", false));
        
        return Map.of(
            "pending", pending,
            "retrying", retrying,
            "failed", failed,
            "success", success,
            "total", pending + retrying + failed + success
        );
    }

    private String truncate(String str, int maxLength) {
        if (Strings.isBlank(str)) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 4000) break;
        }
        return sb.toString();
    }
}

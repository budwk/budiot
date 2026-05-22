package com.budwk.sp.msg.services.impl;

import com.budwk.sp.msg.dto.MsgSendDTO;
import com.budwk.sp.msg.dto.MsgVerifyCodeCheckDTO;
import com.budwk.sp.msg.dto.MsgVerifyCodeSendDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.entity.Msg_template;
import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.msg.services.MsgSendService;
import com.budwk.sp.msg.services.MsgTemplateService;
import com.budwk.sp.msg.services.MsgVerifyCodeService;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.cache.service.WkCacheService;
import jakarta.servlet.http.HttpServletRequest;
import org.nutz.lang.Strings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MsgVerifyCodeServiceImpl implements MsgVerifyCodeService {
    private static final int SMS_CODE_MIN_LENGTH = 4;
    private static final int SMS_CODE_MAX_LENGTH = 6;
    private static final String VERIFY_CODE_PREFIX = "wk:msg:verify:";
    private static final String VERIFY_LOCK_PREFIX = "wk:msg:lock:";
    private static final String VERIFY_SMS_RECEIVER_LIMIT_PREFIX = "wk:msg:sms:limit:receiver:";
    private static final String VERIFY_SMS_IP_LIMIT_PREFIX = "wk:msg:sms:limit:ip:";
    private static final long SMS_RECEIVER_LIMIT_SECONDS = 1800;
    private static final int SMS_RECEIVER_LIMIT_COUNT = 5;
    private static final long SMS_IP_LIMIT_SECONDS = 600;
    private static final int SMS_IP_LIMIT_COUNT = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final MsgChannelService msgChannelService;
    private final MsgTemplateService msgTemplateService;
    private final MsgSendService msgSendService;
    private final WkCacheService wkCacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    public MsgVerifyCodeServiceImpl(MsgChannelService msgChannelService,
                                    MsgTemplateService msgTemplateService,
                                    MsgSendService msgSendService,
                                    WkCacheService wkCacheService,
                                    RedisTemplate<String, Object> redisTemplate) {
        this.msgChannelService = msgChannelService;
        this.msgTemplateService = msgTemplateService;
        this.msgSendService = msgSendService;
        this.wkCacheService = wkCacheService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void sendSmsCode(MsgVerifyCodeSendDTO dto) {
        int codeLength = dto.getLength() == null ? 6 : dto.getLength();
        if (codeLength < SMS_CODE_MIN_LENGTH || codeLength > SMS_CODE_MAX_LENGTH) {
            throw new BaseException("短信验证码长度必须为4到6位数字");
        }
        String tenantId = resolveTenantId(dto.getTenantId());
        String receiver = normalizeReceiver(dto.getReceiver());
        SendGuardReservation reservation = reserveSendGuards(tenantId, receiver, MsgChannelType.SMS, 60);
        try {
            sendCode(dto, tenantId, receiver, MsgChannelType.SMS, 300, codeLength);
        } catch (RuntimeException e) {
            releaseReservation(reservation);
            throw e;
        }
    }

    @Override
    public void sendEmailCode(MsgVerifyCodeSendDTO dto) {
        String tenantId = resolveTenantId(dto.getTenantId());
        String receiver = normalizeReceiver(dto.getReceiver());
        SendGuardReservation reservation = reserveSendGuards(tenantId, receiver, MsgChannelType.EMAIL, 60);
        try {
            sendCode(dto, tenantId, receiver, MsgChannelType.EMAIL, 900, dto.getLength() == null ? 6 : dto.getLength());
        } catch (RuntimeException e) {
            releaseReservation(reservation);
            throw e;
        }
    }

    @Override
    public void checkCode(MsgVerifyCodeCheckDTO dto) {
        String tenantId = resolveTenantId(dto.getTenantId());
        String key = buildCacheKey(tenantId, normalizeReceiver(dto.getReceiver()));
        String codeFromCache = Strings.sNull(wkCacheService.getCache(key));
        if (Strings.isBlank(dto.getCode())) {
            throw new BaseException("请输入验证码");
        }
        if (Strings.isBlank(codeFromCache)) {
            throw new BaseException("验证码已过期");
        }
        if (!dto.getCode().equalsIgnoreCase(codeFromCache)) {
            throw new BaseException("验证码不正确");
        }
        wkCacheService.deleteCache(key);
    }

    private void sendCode(MsgVerifyCodeSendDTO dto, String tenantId, String receiver, MsgChannelType channelType, long ttlSeconds, int codeLength) {
        Msg_channel channel = resolveChannel(dto, tenantId, channelType);
        Msg_template template = resolveTemplate(dto, tenantId, channel);
        String code = generateNumberCode(codeLength);
        MsgSendDTO sendDTO = new MsgSendDTO();
        sendDTO.setTenantId(tenantId);
        sendDTO.setChannelId(channel.getId());
        sendDTO.setTemplateId(template == null ? null : template.getId());
        sendDTO.setBizType(template == null ? MsgTemplateBizType.VERIFY_CODE : template.getBizType());
        sendDTO.setTitle(dto.getSubject());
        sendDTO.setContent(template == null ? "您的验证码为${code}，请在有效期内使用。" : null);
        sendDTO.setParamsJson("{\"code\":\"" + code + "\"}");
        sendDTO.setReceivers(new String[]{receiver});
        msgSendService.send(sendDTO, Strings.sBlank(dto.getLoginname(), "system"), tenantId);
        wkCacheService.setCache(buildCacheKey(tenantId, receiver), code, ttlSeconds, TimeUnit.SECONDS);
    }

    private Msg_channel resolveChannel(MsgVerifyCodeSendDTO dto, String tenantId, MsgChannelType channelType) {
        if (Strings.isNotBlank(dto.getChannelId())) {
            return msgChannelService.getChannel(dto.getChannelId(), tenantId);
        }
        Msg_channel channel = msgChannelService.getDefaultChannel(tenantId, channelType.name());
        if (channel == null) {
            throw new BaseException((channelType == MsgChannelType.SMS ? "短信" : "邮箱") + "渠道未配置");
        }
        return channel;
    }

    private Msg_template resolveTemplate(MsgVerifyCodeSendDTO dto, String tenantId, Msg_channel channel) {
        if (Strings.isNotBlank(dto.getTemplateId())) {
            return msgTemplateService.getTemplate(dto.getTemplateId(), tenantId);
        }
        return msgTemplateService.getDefaultTemplate(tenantId, channel.getId(), MsgTemplateBizType.VERIFY_CODE);
    }

    private SendGuardReservation reserveSendGuards(String tenantId, String receiver, MsgChannelType channelType, long lockSeconds) {
        String lockKey = buildLockKey(tenantId, receiver);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Boolean locked = valueOperations.setIfAbsent(lockKey, "1", lockSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BaseException("请1分钟之后再试");
        }
        List<String> reservedCounters = new ArrayList<>();
        try {
            if (channelType == MsgChannelType.SMS) {
                reserveCounter(valueOperations,
                        buildSmsReceiverLimitKey(tenantId, receiver),
                        SMS_RECEIVER_LIMIT_COUNT,
                        SMS_RECEIVER_LIMIT_SECONDS,
                        "该手机号发送验证码次数过多，请稍后再试");
                reservedCounters.add(buildSmsReceiverLimitKey(tenantId, receiver));
                String clientIp = getClientIp();
                if (Strings.isNotBlank(clientIp)) {
                    reserveCounter(valueOperations,
                            buildSmsIpLimitKey(tenantId, clientIp),
                            SMS_IP_LIMIT_COUNT,
                            SMS_IP_LIMIT_SECONDS,
                            "当前请求发送验证码过于频繁，请稍后再试");
                    reservedCounters.add(buildSmsIpLimitKey(tenantId, clientIp));
                }
            }
            return new SendGuardReservation(lockKey, reservedCounters);
        } catch (RuntimeException e) {
            releaseReservation(new SendGuardReservation(lockKey, reservedCounters));
            throw e;
        }
    }

    private void reserveCounter(ValueOperations<String, Object> valueOperations,
                                String key,
                                int limit,
                                long ttlSeconds,
                                String errorMessage) {
        Long current = valueOperations.increment(key);
        if (current == null) {
            throw new IllegalStateException("验证码发送计数失败");
        }
        if (current == 1L) {
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        if (current > limit) {
            decrementCounter(key);
            throw new BaseException(errorMessage);
        }
    }

    private void releaseReservation(SendGuardReservation reservation) {
        if (reservation == null) {
            return;
        }
        if (Strings.isNotBlank(reservation.lockKey())) {
            redisTemplate.delete(reservation.lockKey());
        }
        for (String counterKey : reservation.counterKeys()) {
            decrementCounter(counterKey);
        }
    }

    private void decrementCounter(String key) {
        Long current = redisTemplate.opsForValue().decrement(key);
        if (current != null && current <= 0) {
            redisTemplate.delete(key);
        }
    }

    private String buildCacheKey(String tenantId, String receiver) {
        return VERIFY_CODE_PREFIX + tenantId + ':' + receiver;
    }

    private String buildLockKey(String tenantId, String receiver) {
        return VERIFY_LOCK_PREFIX + tenantId + ':' + receiver;
    }

    private String buildSmsReceiverLimitKey(String tenantId, String receiver) {
        return VERIFY_SMS_RECEIVER_LIMIT_PREFIX + tenantId + ':' + receiver;
    }

    private String buildSmsIpLimitKey(String tenantId, String clientIp) {
        return VERIFY_SMS_IP_LIMIT_PREFIX + tenantId + ':' + clientIp;
    }

    private String resolveTenantId(String tenantId) {
        return Strings.sBlank(tenantId, GlobalConstant.TENANT_ID_DEFAULT);
    }

    private String normalizeReceiver(String receiver) {
        return Strings.sNull(receiver).trim();
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (Strings.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (Strings.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (Strings.isBlank(ip)) {
            return "";
        }
        if (ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip.trim();
    }

    private String generateNumberCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private record SendGuardReservation(String lockKey, List<String> counterKeys) {
    }
}

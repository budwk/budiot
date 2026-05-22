package com.budwk.sp.msg.services.impl;

import com.budwk.sp.msg.dto.MsgVerifyCodeSendDTO;
import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.enums.MsgChannelType;
import com.budwk.sp.msg.services.MsgChannelService;
import com.budwk.sp.msg.services.MsgSendService;
import com.budwk.sp.msg.services.MsgTemplateService;
import com.budwk.sp.starter.cache.service.WkCacheService;
import com.budwk.sp.starter.common.exception.BaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MsgVerifyCodeServiceImplTest {
    private static final String TENANT_ID = "platform";
    private static final String RECEIVER = "13800000000";
    private static final String RECEIVER_LIMIT_KEY = "wk:msg:sms:limit:receiver:" + TENANT_ID + ':' + RECEIVER;
    private static final String LOCK_KEY = "wk:msg:lock:" + TENANT_ID + ':' + RECEIVER;
    private static final String IP = "10.0.0.8";
    private static final String IP_LIMIT_KEY = "wk:msg:sms:limit:ip:" + TENANT_ID + ':' + IP;

    @Mock
    private MsgChannelService msgChannelService;
    @Mock
    private MsgTemplateService msgTemplateService;
    @Mock
    private MsgSendService msgSendService;
    @Mock
    private WkCacheService wkCacheService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private MsgVerifyCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MsgVerifyCodeServiceImpl(msgChannelService, msgTemplateService, msgSendService, wkCacheService, redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        mockRequest(IP);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldSendSmsCodeWhenUnderGuardThreshold() {
        mockDefaultSmsChannel();
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(Boolean.TRUE);
        when(valueOperations.increment(RECEIVER_LIMIT_KEY)).thenReturn(1L);
        when(valueOperations.increment(IP_LIMIT_KEY)).thenReturn(1L);

        service.sendSmsCode(buildDto());

        verify(msgSendService).send(any(), eq("system"), eq(TENANT_ID));
        verify(redisTemplate).expire(RECEIVER_LIMIT_KEY, 1800L, TimeUnit.SECONDS);
        verify(redisTemplate).expire(IP_LIMIT_KEY, 600L, TimeUnit.SECONDS);
        verify(wkCacheService).setCache(eq("wk:msg:verify:" + TENANT_ID + ':' + RECEIVER), any(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectSmsCodeWhenCooldownExists() {
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(Boolean.FALSE);

        BaseException exception = assertThrows(BaseException.class, () -> service.sendSmsCode(buildDto()));

        assertEquals("请1分钟之后再试", exception.getMessage());
        verify(msgSendService, never()).send(any(), any(), any());
        verify(valueOperations, never()).increment(any());
    }

    @Test
    void shouldRejectSmsCodeWhenReceiverLimitExceeded() {
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(Boolean.TRUE);
        when(valueOperations.increment(RECEIVER_LIMIT_KEY)).thenReturn(6L);
        when(valueOperations.decrement(RECEIVER_LIMIT_KEY)).thenReturn(5L);

        BaseException exception = assertThrows(BaseException.class, () -> service.sendSmsCode(buildDto()));

        assertEquals("该手机号发送验证码次数过多，请稍后再试", exception.getMessage());
        verify(valueOperations).decrement(RECEIVER_LIMIT_KEY);
        verify(redisTemplate).delete(LOCK_KEY);
        verify(valueOperations, never()).increment(IP_LIMIT_KEY);
        verify(msgSendService, never()).send(any(), any(), any());
    }

    @Test
    void shouldRejectSmsCodeWhenIpLimitExceeded() {
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(Boolean.TRUE);
        when(valueOperations.increment(RECEIVER_LIMIT_KEY)).thenReturn(1L);
        when(valueOperations.increment(IP_LIMIT_KEY)).thenReturn(11L);
        when(valueOperations.decrement(RECEIVER_LIMIT_KEY)).thenReturn(0L);
        when(valueOperations.decrement(IP_LIMIT_KEY)).thenReturn(10L);

        BaseException exception = assertThrows(BaseException.class, () -> service.sendSmsCode(buildDto()));

        assertEquals("当前请求发送验证码过于频繁，请稍后再试", exception.getMessage());
        verify(redisTemplate).expire(RECEIVER_LIMIT_KEY, 1800L, TimeUnit.SECONDS);
        verify(valueOperations).decrement(RECEIVER_LIMIT_KEY);
        verify(valueOperations).decrement(IP_LIMIT_KEY);
        verify(redisTemplate).delete(LOCK_KEY);
        verify(msgSendService, never()).send(any(), any(), any());
    }

    @Test
    void shouldReleaseReservationWhenSendFails() {
        mockDefaultSmsChannel();
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(Boolean.TRUE);
        when(valueOperations.increment(RECEIVER_LIMIT_KEY)).thenReturn(1L);
        when(valueOperations.increment(IP_LIMIT_KEY)).thenReturn(1L);
        when(msgSendService.send(any(), any(), any())).thenThrow(new RuntimeException("send failed"));
        when(valueOperations.decrement(RECEIVER_LIMIT_KEY)).thenReturn(0L);
        when(valueOperations.decrement(IP_LIMIT_KEY)).thenReturn(0L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.sendSmsCode(buildDto()));

        assertEquals("send failed", exception.getMessage());
        verify(valueOperations).decrement(RECEIVER_LIMIT_KEY);
        verify(valueOperations).decrement(IP_LIMIT_KEY);
        verify(redisTemplate).delete(LOCK_KEY);
        verify(redisTemplate).delete(RECEIVER_LIMIT_KEY);
        verify(redisTemplate).delete(IP_LIMIT_KEY);
        verify(wkCacheService, never()).setCache(any(), any(), anyLong(), any());
    }

    private MsgVerifyCodeSendDTO buildDto() {
        MsgVerifyCodeSendDTO dto = new MsgVerifyCodeSendDTO();
        dto.setReceiver(" " + RECEIVER + " ");
        return dto;
    }

    private void mockDefaultSmsChannel() {
        Msg_channel channel = new Msg_channel();
        channel.setId("channel-1");
        channel.setChannelType(MsgChannelType.SMS);
        when(msgChannelService.getDefaultChannel(TENANT_ID, MsgChannelType.SMS.name())).thenReturn(channel);
    }

    private void mockRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", ip);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}

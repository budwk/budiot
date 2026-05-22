package com.budwk.sp.msg.sender.im;

import com.budwk.sp.msg.dto.MsgSendResultDTO;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.sender.MsgChannelSender;
import com.budwk.sp.msg.sender.MsgResolvedReceiver;
import com.budwk.sp.msg.sender.MsgSenderContext;
import com.budwk.sp.msg.sender.config.WeComConfig;
import com.budwk.sp.msg.sender.support.HttpClientSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WeComSender implements MsgChannelSender {
    private final ObjectMapper objectMapper;

    public WeComSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MsgProviderType providerType) {
        return providerType == MsgProviderType.WECOM_BOT;
    }

    @Override
    public MsgSendResultDTO send(MsgSenderContext context, MsgResolvedReceiver receiver) {
        WeComConfig config = readConfig(context);
        Map<String, Object> text = new HashMap<>();
        text.put("content", context.getContent());
        text.put("mentioned_mobile_list", context.getMentions());
        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", "text");
        payload.put("text", text);
        try {
            JsonNode response = HttpClientSupport.postJson(config.getWebhookUrl(), objectMapper.writeValueAsString(payload), null, objectMapper);
            MsgSendResultDTO result = new MsgSendResultDTO();
            result.setReceiver(receiver.getReceiver());
            result.setCode(response.path("errcode").asText());
            result.setMessage(response.path("errmsg").asText());
            result.setSuccess(response.path("errcode").asInt(-1) == 0);
            result.setSendAt(System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("企业微信发送失败: " + e.getMessage(), e);
        }
    }

    private WeComConfig readConfig(MsgSenderContext context) {
        try {
            return objectMapper.readValue(context.getChannel().getConfigJson(), WeComConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("企业微信渠道配置错误: " + e.getMessage(), e);
        }
    }
}

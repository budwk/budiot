package com.budwk.sp.msg.sender.im;

import com.budwk.sp.msg.dto.MsgSendResultDTO;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.sender.MsgChannelSender;
import com.budwk.sp.msg.sender.MsgResolvedReceiver;
import com.budwk.sp.msg.sender.MsgSenderContext;
import com.budwk.sp.msg.sender.config.DingTalkConfig;
import com.budwk.sp.msg.sender.support.HttpClientSupport;
import com.budwk.sp.msg.sender.support.SignHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class DingTalkSender implements MsgChannelSender {
    private final ObjectMapper objectMapper;

    public DingTalkSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MsgProviderType providerType) {
        return providerType == MsgProviderType.DINGTALK_BOT;
    }

    @Override
    public MsgSendResultDTO send(MsgSenderContext context, MsgResolvedReceiver receiver) {
        DingTalkConfig config = readConfig(context);
        String webhookUrl = config.getWebhookUrl();
        if (config.getSecret() != null && !config.getSecret().isBlank()) {
            long timestamp = System.currentTimeMillis();
            String sign = SignHelper.hmacSha256Base64(config.getSecret(), timestamp + "\\n" + config.getSecret());
            webhookUrl = webhookUrl + (webhookUrl.contains("?") ? "&" : "?") + "timestamp=" + timestamp + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);
        }
        Map<String, Object> text = new HashMap<>();
        text.put("content", context.getContent());
        Map<String, Object> at = new HashMap<>();
        at.put("atMobiles", context.getMentions());
        at.put("isAtAll", false);
        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", "text");
        payload.put("text", text);
        payload.put("at", at);
        try {
            JsonNode response = HttpClientSupport.postJson(webhookUrl, objectMapper.writeValueAsString(payload), null, objectMapper);
            MsgSendResultDTO result = new MsgSendResultDTO();
            result.setReceiver(receiver.getReceiver());
            result.setCode(response.path("errcode").asText());
            result.setMessage(response.path("errmsg").asText());
            result.setRequestId(response.path("request_id").asText(null));
            result.setSuccess(response.path("errcode").asInt(-1) == 0);
            result.setSendAt(System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("钉钉发送失败: " + e.getMessage(), e);
        }
    }

    private DingTalkConfig readConfig(MsgSenderContext context) {
        try {
            return objectMapper.readValue(context.getChannel().getConfigJson(), DingTalkConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("钉钉渠道配置错误: " + e.getMessage(), e);
        }
    }
}

package com.budwk.sp.msg.sender.sms;

import com.budwk.sp.msg.dto.MsgSendResultDTO;
import com.budwk.sp.msg.enums.MsgTemplateBizType;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.sender.MsgChannelSender;
import com.budwk.sp.msg.sender.MsgResolvedReceiver;
import com.budwk.sp.msg.sender.MsgSenderContext;
import com.budwk.sp.msg.sender.config.AliyunSmsConfig;
import com.budwk.sp.msg.sender.support.HttpClientSupport;
import com.budwk.sp.msg.sender.support.SignHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Component
public class AliyunSmsSender implements MsgChannelSender {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private final ObjectMapper objectMapper;

    public AliyunSmsSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MsgProviderType providerType) {
        return providerType == MsgProviderType.ALIYUN_SMS;
    }

    @Override
    public MsgSendResultDTO send(MsgSenderContext context, MsgResolvedReceiver receiver) {
        AliyunSmsConfig config = readConfig(context);
        if (context.getTemplate() == null) {
            throw new IllegalArgumentException("阿里云短信发送必须指定模板");
        }
        validateTemplateParams(context);
        try {
            TreeMap<String, String> params = new TreeMap<>();
            params.put("AccessKeyId", config.getAccessKeyId());
            params.put("Action", "SendSms");
            params.put("Format", "JSON");
            params.put("PhoneNumbers", receiver.getReceiver());
            params.put("RegionId", config.getRegionId() == null || config.getRegionId().isBlank() ? "cn-hangzhou" : config.getRegionId());
            params.put("SignName", config.getSignName());
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("SignatureVersion", "1.0");
            params.put("TemplateCode", context.getTemplate().getTemplateCode());
            params.put("TemplateParam", objectMapper.writeValueAsString(context.getParams()));
            params.put("Timestamp", TS_FORMATTER.format(Instant.now()));
            params.put("Version", "2017-05-25");
            String canonicalized = buildCanonicalizedQuery(params);
            String stringToSign = "GET&%2F&" + SignHelper.percentEncode(canonicalized);
            String signature = SignHelper.hmacSha1Base64(config.getAccessKeySecret() + "&", stringToSign);
            params.put("Signature", signature);
            JsonNode response = HttpClientSupport.getJson(resolveEndpoint(config), params, objectMapper);
            MsgSendResultDTO result = new MsgSendResultDTO();
            result.setReceiver(receiver.getReceiver());
            result.setRequestId(response.path("RequestId").asText(null));
            result.setCode(response.path("Code").asText());
            result.setMessage(response.path("Message").asText());
            result.setSuccess("OK".equalsIgnoreCase(result.getCode()));
            result.setSendAt(System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("阿里云短信发送失败: " + e.getMessage(), e);
        }
    }

    private String buildCanonicalizedQuery(TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(SignHelper.percentEncode(entry.getKey())).append('=').append(SignHelper.percentEncode(entry.getValue()));
            first = false;
        }
        return sb.toString();
    }

    private String resolveEndpoint(AliyunSmsConfig config) {
        return config.getEndpoint() == null || config.getEndpoint().isBlank() ? "https://dysmsapi.aliyuncs.com/" : config.getEndpoint();
    }

    private AliyunSmsConfig readConfig(MsgSenderContext context) {
        try {
            return objectMapper.readValue(context.getChannel().getConfigJson(), AliyunSmsConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("阿里云短信配置错误: " + e.getMessage(), e);
        }
    }

    private void validateTemplateParams(MsgSenderContext context) {
        if (context.getTemplate() == null || context.getTemplate().getBizType() != MsgTemplateBizType.VERIFY_CODE) {
            return;
        }
        Object code = context.getParams().get("code");
        String codeText = code == null ? "" : String.valueOf(code).trim();
        if (!codeText.matches("\\d{4,6}")) {
            throw new IllegalArgumentException("阿里云验证码模板参数 code 必须为4到6位数字");
        }
    }
}

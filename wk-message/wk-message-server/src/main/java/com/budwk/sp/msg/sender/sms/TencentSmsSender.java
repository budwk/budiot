package com.budwk.sp.msg.sender.sms;

import com.budwk.sp.msg.dto.MsgSendResultDTO;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.sender.MsgChannelSender;
import com.budwk.sp.msg.sender.MsgResolvedReceiver;
import com.budwk.sp.msg.sender.MsgSenderContext;
import com.budwk.sp.msg.sender.config.TencentSmsConfig;
import com.budwk.sp.msg.sender.support.HttpClientSupport;
import com.budwk.sp.msg.sender.support.SignHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Component
public class TencentSmsSender implements MsgChannelSender {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private final ObjectMapper objectMapper;

    public TencentSmsSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MsgProviderType providerType) {
        return providerType == MsgProviderType.TENCENT_SMS;
    }

    @Override
    public MsgSendResultDTO send(MsgSenderContext context, MsgResolvedReceiver receiver) {
        if (context.getTemplate() == null) {
            throw new IllegalArgumentException("腾讯云短信发送必须指定模板");
        }
        TencentSmsConfig config = readConfig(context);
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("SmsSdkAppId", config.getSdkAppId());
            payload.put("SignName", config.getSignName());
            payload.put("TemplateId", context.getTemplate().getTemplateCode());
            payload.put("PhoneNumberSet", List.of(normalizePhone(receiver.getReceiver())));
            List<String> params = new ArrayList<>();
            for (Object value : context.getParams().values()) {
                params.add(value == null ? "" : String.valueOf(value));
            }
            payload.put("TemplateParamSet", params);
            String body = objectMapper.writeValueAsString(payload);
            long timestamp = System.currentTimeMillis() / 1000;
            String date = DATE_FORMATTER.format(Instant.ofEpochSecond(timestamp));
            String host = config.getEndpoint() == null || config.getEndpoint().isBlank() ? "sms.tencentcloudapi.com" : config.getEndpoint();
            String canonicalRequest = "POST\\n/\\n\\ncontent-type:application/json; charset=utf-8\\nhost:" + host + "\\n\\ncontent-type;host\\n" + sha256Hex(body);
            String credentialScope = date + "/sms/tc3_request";
            String stringToSign = "TC3-HMAC-SHA256\\n" + timestamp + "\\n" + credentialScope + "\\n" + sha256Hex(canonicalRequest);
            byte[] secretDate = SignHelper.hmacSha256Bytes(("TC3" + config.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
            byte[] secretService = SignHelper.hmacSha256Bytes(secretDate, "sms");
            byte[] secretSigning = SignHelper.hmacSha256Bytes(secretService, "tc3_request");
            String signature = HexFormat.of().formatHex(SignHelper.hmacSha256Bytes(secretSigning, stringToSign));
            String authorization = "TC3-HMAC-SHA256 Credential=" + config.getSecretId() + "/" + credentialScope + ", SignedHeaders=content-type;host, Signature=" + signature;
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", authorization);
            headers.put("Host", host);
            headers.put("X-TC-Action", "SendSms");
            headers.put("X-TC-Version", "2021-01-11");
            headers.put("X-TC-Region", config.getRegion() == null || config.getRegion().isBlank() ? "ap-guangzhou" : config.getRegion());
            headers.put("X-TC-Timestamp", String.valueOf(timestamp));
            JsonNode response = HttpClientSupport.postJson("https://" + host, body, headers, objectMapper).path("Response");
            JsonNode sendStatus = response.path("SendStatusSet").isArray() && response.path("SendStatusSet").size() > 0 ? response.path("SendStatusSet").get(0) : objectMapper.createObjectNode();
            MsgSendResultDTO result = new MsgSendResultDTO();
            result.setReceiver(receiver.getReceiver());
            result.setRequestId(response.path("RequestId").asText(null));
            result.setCode(sendStatus.path("Code").asText());
            result.setMessage(sendStatus.path("Message").asText());
            result.setSuccess("Ok".equalsIgnoreCase(result.getCode()));
            result.setSendAt(System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("腾讯云短信发送失败: " + e.getMessage(), e);
        }
    }

    private String normalizePhone(String phone) {
        return phone.startsWith("+") ? phone : "+86" + phone;
    }

    private String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("sha256 error", e);
        }
    }

    private TencentSmsConfig readConfig(MsgSenderContext context) {
        try {
            return objectMapper.readValue(context.getChannel().getConfigJson(), TencentSmsConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("腾讯云短信配置错误: " + e.getMessage(), e);
        }
    }
}

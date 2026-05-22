package com.budwk.sp.msg.sender.email;

import com.budwk.sp.msg.dto.MsgSendResultDTO;
import com.budwk.sp.msg.enums.MsgProviderType;
import com.budwk.sp.msg.sender.MsgChannelSender;
import com.budwk.sp.msg.sender.MsgResolvedReceiver;
import com.budwk.sp.msg.sender.MsgSenderContext;
import com.budwk.sp.msg.sender.config.EmailConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nutz.lang.Strings;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class EmailSender implements MsgChannelSender {
    private final ObjectMapper objectMapper;

    public EmailSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MsgProviderType providerType) {
        return providerType == MsgProviderType.SMTP;
    }

    @Override
    public MsgSendResultDTO send(MsgSenderContext context, MsgResolvedReceiver receiver) {
        EmailConfig config = readConfig(context);
        validateConfig(config);
        int port = config.getPort() == null ? 25 : config.getPort();
        boolean sslEnabled = resolveSsl(config, port);
        boolean starttlsEnabled = resolveStarttls(config, port, sslEnabled);
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(port);
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        sender.setProtocol("smtp");
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnabled));
        props.put("mail.smtp.starttls.required", String.valueOf(starttlsEnabled));
        props.put("mail.smtp.ssl.enable", String.valueOf(sslEnabled));
        if (sslEnabled) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.smtp.ssl.trust", config.getHost());
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(resolveFrom(config));
        mail.setTo(receiver.getReceiver());
        mail.setSubject(context.getTitle());
        mail.setText(context.getContent());
        try {
            sender.send(mail);
        } catch (MailAuthenticationException e) {
            throw new IllegalArgumentException("SMTP认证失败: " + getRootMessage(e) + "。请确认用户名填写完整邮箱地址、密码使用授权码，并检查邮箱已开启SMTP服务", e);
        } catch (MailSendException e) {
            throw new IllegalArgumentException("SMTP发送失败: " + getRootMessage(e), e);
        }

        MsgSendResultDTO result = new MsgSendResultDTO();
        result.setSuccess(true);
        result.setReceiver(receiver.getReceiver());
        result.setCode("OK");
        result.setMessage("邮件发送成功");
        result.setSendAt(System.currentTimeMillis());
        return result;
    }

    private EmailConfig readConfig(MsgSenderContext context) {
        try {
            return objectMapper.readValue(context.getChannel().getConfigJson(), EmailConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("邮箱渠道配置错误: " + e.getMessage(), e);
        }
    }

    private void validateConfig(EmailConfig config) {
        if (Strings.isBlank(config.getHost())) {
            throw new IllegalArgumentException("邮箱渠道配置错误: SMTP Host不能为空");
        }
        if (Strings.isBlank(config.getUsername())) {
            throw new IllegalArgumentException("邮箱渠道配置错误: SMTP账号不能为空");
        }
        if (Strings.isBlank(config.getPassword())) {
            throw new IllegalArgumentException("邮箱渠道配置错误: SMTP密码/授权码不能为空");
        }
    }

    private boolean resolveSsl(EmailConfig config, int port) {
        if (port == 465) {
            return true;
        }
        if (port == 587) {
            return false;
        }
        return Boolean.TRUE.equals(config.getSsl());
    }

    private boolean resolveStarttls(EmailConfig config, int port, boolean sslEnabled) {
        if (port == 465) {
            return false;
        }
        if (port == 587) {
            return true;
        }
        return !sslEnabled && Boolean.TRUE.equals(config.getStarttls());
    }

    private String resolveFrom(EmailConfig config) {
        if (Strings.isBlank(config.getFrom())) {
            return config.getUsername();
        }
        return config.getFrom();
    }

    private String getRootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String rootMessage = Strings.sBlank(root.getMessage(), null);
        if (Strings.isNotBlank(rootMessage)) {
            return rootMessage;
        }
        String message = Strings.sBlank(e.getMessage(), null);
        return Strings.isNotBlank(message) ? message : "认证失败";
    }
}

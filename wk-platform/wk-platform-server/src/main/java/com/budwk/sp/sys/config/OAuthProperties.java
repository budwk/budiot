package com.budwk.sp.sys.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wk.oauth")
public class OAuthProperties {
    private Provider qq = new Provider();
    private Provider wechat = new Provider();
    private Provider alipay = new Provider();

    public Provider getQq() {
        return qq;
    }

    public void setQq(Provider qq) {
        this.qq = qq;
    }

    public Provider getWechat() {
        return wechat;
    }

    public void setWechat(Provider wechat) {
        this.wechat = wechat;
    }

    public Provider getAlipay() {
        return alipay;
    }

    public void setAlipay(Provider alipay) {
        this.alipay = alipay;
    }

    @Data
    public static class Provider {
        private boolean enabled;
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String alipayPublicKey;
        private boolean unionId;
        private List<String> scopes = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getAlipayPublicKey() {
            return alipayPublicKey;
        }

        public void setAlipayPublicKey(String alipayPublicKey) {
            this.alipayPublicKey = alipayPublicKey;
        }

        public boolean isUnionId() {
            return unionId;
        }

        public void setUnionId(boolean unionId) {
            this.unionId = unionId;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }
    }
}

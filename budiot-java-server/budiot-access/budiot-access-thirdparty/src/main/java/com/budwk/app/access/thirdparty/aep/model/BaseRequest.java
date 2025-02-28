package com.budwk.app.access.thirdparty.aep.model;

import org.nutz.http.Request;
import org.nutz.lang.util.NutMap;

import java.io.Serializable;
import java.util.Arrays;

/**
 * API 请求 Model
 *
 */
public class BaseRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    protected NutMap header = new NutMap();
    protected NutMap params = new NutMap();
    protected byte[] body;
    private Request.METHOD method;
    /**
     * 默认超时时间 3000 ms
     */
    private int timeout = 3000;

    /**
     * 应用id
     */
    private String appId;
    /**
     * 应用Key
     */
    private String appKey;
    /**
     * 应用密钥
     */
    private String appSecret;
    /**
     * 基础 URL
     */
    private String baseUrl = "http://ag-api.ctwing.cn";
    private String path;

    public BaseRequest() {
    }

    public BaseRequest(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        header.put("application", appKey);
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        header.put("sdk", "0");
    }

    public BaseRequest(String baseUrl, String appKey, String appSecret) {
        this.baseUrl = baseUrl;
        this.appKey = appKey;
        this.appSecret = appSecret;
        header.put("application", appKey);
        header.put("timestamp", String.valueOf(System.currentTimeMillis()));
        header.put("sdk", "0");
    }

    public Request.METHOD getMethod() {
        return method;
    }

    public void setMethod(Request.METHOD method) {
        this.method = method;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public NutMap getHeader() {
        return header;
    }

    public void setHeader(String key, String value) {
        this.header.put(key, value);
    }

    public NutMap getParams() {
        return params;
    }

    public void setParams(NutMap params) {
        this.params.putAll(params);
    }

    public void setParam(String key, String value) {
        this.params.put(key, value);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "header=" + header +
                ", params=" + params +
                ", body=" + Arrays.toString(body) +
                ", appId='" + appId + '\'' +
                ", appKey='" + appKey + '\'' +
                ", appSecret='" + appSecret + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                '}';
    }
}

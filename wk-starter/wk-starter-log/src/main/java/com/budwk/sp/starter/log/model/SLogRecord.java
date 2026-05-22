package com.budwk.sp.starter.log.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 操作日志上下文
 *
 * @author wizzer@qq.com
 */
@Data
@Builder
public class SLogRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long createdAt;
    private String tenantId;
    private String appId;
    private String userId;
    private String loginname;
    private String username;
    private String type;
    private String tag;
    private String msg;
    private String url;
    private String method;
    private String ip;
    private String browser;
    private String os;
    private String params;
    private Map<String, Object> paramsMap;
    private String result;
    private String exception;
    private long executeTime;
}

package com.budwk.sp.starter.common.constant;

/**
 * Redis 缓存 Key 常量
 *
 * @author wizzer@qq.com
 */
public class RedisConstant {

    /**
     * 缓存前缀
     */
    public static final String PRE = "wk:sp:";

    /**
     * 用户中心 - 验证码
     */
    public static final String UCENTER_CAPTCHA = PRE + "ucenter:captcha:";

    /**
     * 用户中心 - 短信验证码
     */
    public static final String UCENTER_SMSCODE = PRE + "ucenter:smscode:";

    /**
     * 用户中心 - 邮件验证码
     */
    public static final String UCENTER_EMAILCODE = PRE + "ucenter:emailcode:";

    /**
     * 用户中心 - OAuth state
     */
    public static final String UCENTER_OAUTH_STATE = PRE + "ucenter:oauth:state:";

    /**
     * 用户中心 - OAuth state context
     */
    public static final String UCENTER_OAUTH_STATE_CTX = PRE + "ucenter:oauth:state:ctx:";

    /**
     * 用户中心 - OAuth bind ticket
     */
    public static final String UCENTER_OAUTH_BIND_TICKET = PRE + "ucenter:oauth:bind:";

    /**
     * 用户中心 - OAuth login ticket
     */
    public static final String UCENTER_OAUTH_LOGIN_TICKET = PRE + "ucenter:oauth:login:";
}

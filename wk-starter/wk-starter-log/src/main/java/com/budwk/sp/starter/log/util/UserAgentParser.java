package com.budwk.sp.starter.log.util;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 简单 User-Agent 解析
 *
 * @author wizzer@qq.com
 */
public class UserAgentParser {

    private UserAgentParser() {
    }

    public static UserAgentInfo parse(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return new UserAgentInfo("未知浏览器", "未知系统");
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        return new UserAgentInfo(resolveBrowser(ua), resolveOs(ua));
    }

    private static String resolveBrowser(String ua) {
        if (ua.contains("edg/")) {
            return "Microsoft Edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        }
        if (ua.contains("chrome/")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        }
        if (ua.contains("micromessenger/")) {
            return "微信";
        }
        if (ua.contains("trident/") || ua.contains("msie")) {
            return "Internet Explorer";
        }
        return "未知浏览器";
    }

    private static String resolveOs(String ua) {
        if (ua.contains("windows nt 10")) {
            return "Windows 10/11";
        }
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "未知系统";
    }
}

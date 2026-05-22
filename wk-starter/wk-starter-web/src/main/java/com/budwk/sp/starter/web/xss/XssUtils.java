package com.budwk.sp.starter.web.xss;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public class XssUtils {
    /**
     * 清洗字符串，移除潜在的 XSS 脚本
     */
    public static String clean(String content) {
        if (content == null) {
            return null;
        }
        // 使用 simpleText 策略，不允许任何 HTML 标签
        // 如果需要允许部分标签（如 <b>），可调整为 Safelist.basic()
        return Jsoup.clean(content, "", Safelist.none(), new Document.OutputSettings().prettyPrint(false));
    }
}

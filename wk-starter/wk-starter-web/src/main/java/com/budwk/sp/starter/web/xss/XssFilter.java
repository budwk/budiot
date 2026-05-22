package com.budwk.sp.starter.web.xss;

import com.budwk.sp.starter.web.config.WkXssProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import java.io.IOException;
import java.util.List;

public class XssFilter implements Filter {

    private final WkXssProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public XssFilter(WkXssProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getServletPath();

        // 检查是否在排除名单中
        if (isExclude(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 包装请求进行脱敏
        chain.doFilter(new XssRequestWrapper(req), response);
    }

    private boolean isExclude(String path) {
        List<String> excludes = properties.getExcludeUrls();
        if (excludes == null || excludes.isEmpty()) {
            return false;
        }
        return excludes.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
package com.budwk.sp.starter.web.xss;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class XssRequestWrapper extends HttpServletRequestWrapper {
    public XssRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        return XssUtils.clean(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        for (int i = 0; i < values.length; i++) {
            values[i] = XssUtils.clean(values[i]);
        }
        return values;
    }

    @Override
    public String getHeader(String name) {
        return XssUtils.clean(super.getHeader(name));
    }
}
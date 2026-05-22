package com.budwk.sp.starter.web.param;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JsonBodyParamFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public JsonBodyParamFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"PATCH".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method)) {
            return true;
        }
        String contentType = request.getContentType();
        if (contentType == null) {
            return true;
        }
        return !contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new JsonBodyParamRequestWrapper(request, objectMapper), response);
    }
}

package com.budwk.sp.starter.web.param;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public class JsonBodyParamRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;
    private final Map<String, String[]> parameterMap;
    private final ObjectMapper objectMapper;

    public JsonBodyParamRequestWrapper(HttpServletRequest request, ObjectMapper objectMapper) throws IOException {
        super(request);
        this.objectMapper = objectMapper;
        this.body = StreamUtils.copyToByteArray(request.getInputStream());
        this.parameterMap = new LinkedHashMap<>(request.getParameterMap());
        mergeJsonBodyParameters();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), resolveCharset()));
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameterMap.get(name);
        return values == null || values.length == 0 ? null : values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(parameterMap);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return new Vector<>(parameterMap.keySet()).elements();
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    private void mergeJsonBodyParameters() throws IOException {
        if (body.length == 0) {
            return;
        }
        JsonNode root = objectMapper.readTree(body);
        if (root == null || !root.isObject()) {
            return;
        }
        root.fields().forEachRemaining(entry -> {
            if (parameterMap.containsKey(entry.getKey())) {
                return;
            }
            String[] values = toParameterValues(entry.getValue());
            if (values != null) {
                parameterMap.put(entry.getKey(), values);
            }
        });
    }

    private String[] toParameterValues(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            String[] values = new String[node.size()];
            for (int i = 0; i < node.size(); i++) {
                values[i] = toSingleValue(node.get(i));
            }
            return values;
        }
        return new String[]{toSingleValue(node)};
    }

    private String toSingleValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isValueNode()) {
            return node.asText();
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize JSON request parameter", e);
        }
    }

    private Charset resolveCharset() {
        String encoding = getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(encoding);
    }
}

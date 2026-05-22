package com.budwk.sp.msg.sender.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

public final class HttpClientSupport {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private HttpClientSupport() {
    }

    public static JsonNode postJson(String url, String json, Map<String, String> headers, ObjectMapper objectMapper) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (headers != null) {
            headers.forEach(builder::header);
        }
        HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return objectMapper.readTree(response.body());
    }

    public static JsonNode getJson(String url, Map<String, String> query, ObjectMapper objectMapper) throws IOException, InterruptedException {
        StringJoiner joiner = new StringJoiner("&");
        if (query != null) {
            query.forEach((key, value) -> joiner.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));
        }
        String requestUrl = query == null || query.isEmpty() ? url : url + "?" + joiner;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return objectMapper.readTree(response.body());
    }
}

package cn.dev33.satoken.reactor.model;

import cn.dev33.satoken.context.model.SaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.net.URI;

/**
 * Sa-Token SaResponseForReactor 覆盖类
 * <p>
 * 修复 Sa-Token 1.45.0 在 Spring Boot 4 / Spring Framework 7 下的兼容性问题：
 * 原始类字节码中引用了 ServerHttpResponse.setStatusCode(HttpStatus)，
 * 而 Spring Framework 7 中该方法签名变为 setStatusCode(HttpStatusCode)，
 * 导致运行时 NoSuchMethodError。
 */
public class SaResponseForReactor implements SaResponse {

    protected ServerHttpResponse response;

    public SaResponseForReactor(ServerHttpResponse response) {
        this.response = response;
    }

    @Override
    public Object getSource() {
        return response;
    }

    @Override
    public SaResponse setStatus(int status) {
        HttpStatusCode statusCode = HttpStatusCode.valueOf(status);
        response.setStatusCode(statusCode);
        return this;
    }

    @Override
    public SaResponse setHeader(String name, String value) {
        response.getHeaders().set(name, value);
        return this;
    }

    @Override
    public SaResponse addHeader(String name, String value) {
        response.getHeaders().add(name, value);
        return this;
    }

    @Override
    public Object redirect(String url) {
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create(url));
        return null;
    }
}

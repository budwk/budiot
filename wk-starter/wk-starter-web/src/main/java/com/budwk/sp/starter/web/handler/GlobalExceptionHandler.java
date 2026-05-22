package com.budwk.sp.starter.web.handler;

import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.starter.common.result.Result;
import com.budwk.sp.starter.web.config.WkExceptionProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 表单验证等异常
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final String GENERIC_SERVER_ERROR_MESSAGE = "系统服务异常，请联系管理员";
    private final WkExceptionProperties exceptionProperties;

    /**
     * 处理实体类校验异常 (如 @RequestBody 接收的对象)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleBindException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors()
                .stream()
                .map(ex -> ex.getField() + ": " + ex.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("请求处理失败 [{}] {} handler={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), resolveHandler(request),
                e.getClass().getName(), msg, e);
        return Result.error(400, msg);
    }

    /**
     * 处理路径参数或查询参数校验异常 (如 @Min(1) Long id)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Object> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        log.warn("请求处理失败 [{}] {} handler={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), resolveHandler(request),
                e.getClass().getName(), e.getMessage(), e);
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException e,
                                                                        HttpServletRequest request) {
        String msg = "缺少请求参数: " + e.getParameterName();
        log.warn("请求处理失败 [{}] {} handler={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), resolveHandler(request),
                e.getClass().getName(), e.getMessage(), e);
        return Result.error(400, msg);
    }

    @ExceptionHandler(BaseException.class)
    public Result<Object> handleBaseError(Exception e, HttpServletRequest request) {
        log.error("请求处理异常 [{}] {} handler={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), resolveHandler(request),
                e.getClass().getName(), e.getMessage(), e);
        return Result.error(403, e.getMessage());
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleRuntimeError(Exception e, HttpServletRequest request, HttpServletResponse response) {
        Throwable saTokenException = findSaTokenException(e);
        if (saTokenException != null) {
            log.warn("请求处理失败 [{}] {} handler={} exception={} message={}",
                    request.getMethod(), request.getRequestURI(), resolveHandler(request),
                    saTokenException.getClass().getName(), saTokenException.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error(HttpServletResponse.SC_UNAUTHORIZED, "登录已失效，请重新登录");
        }
        log.error("请求处理异常 [{}] {} handler={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), resolveHandler(request),
                e.getClass().getName(), e.getMessage(), e);
        return Result.error(500, buildServerErrorMessage(e));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("资源未找到 [{}] {} handler={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), resolveHandler(request),
                e.getClass().getName(), e.getMessage(), e);
        return Result.error(404, "Resource not found: " + e.getResourcePath());
    }

    private String resolveHandler(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getBeanType().getName() + "#" + handlerMethod.getMethod().getName();
        }
        return "N/A";
    }

    private String buildServerErrorMessage(Exception e) {
        if (exceptionProperties != null && exceptionProperties.isExposeMessage()) {
            return "系统服务异常: " + e.getMessage();
        }
        return GENERIC_SERVER_ERROR_MESSAGE;
    }

    private Throwable findSaTokenException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            if ("cn.dev33.satoken.exception.SaTokenException".equals(className)
                    || "cn.dev33.satoken.exception.SaTokenContextException".equals(className)
                    || "cn.dev33.satoken.exception.NotLoginException".equals(className)
                    || "cn.dev33.satoken.exception.NotPermissionException".equals(className)
                    || "cn.dev33.satoken.exception.NotRoleException".equals(className)) {
                return current;
            }
            current = current.getCause();
        }
        return null;
    }
}

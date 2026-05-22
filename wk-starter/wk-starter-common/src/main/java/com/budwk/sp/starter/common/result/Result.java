package com.budwk.sp.starter.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.nutz.lang.Strings;
import java.io.Serializable;

/**
 * @author wizzer@qq.com
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = -1L;

    private static final int SUCCESS_CODE = 200;
    private static final int ERROR_CODE = 500;

    private int code;
    private String msg;
    private long time;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static <T> Result<T> NEW() {
        return new Result<>();
    }

    private Result() {
        this.time = System.currentTimeMillis();
    }

    private Result(int resultCode, String msg) {
        this(resultCode, null, msg);
    }

    private Result(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
        this.time = System.currentTimeMillis();
    }

    public Result<T> addCode(int code) {
        this.code = code;
        return this;
    }

    public Result<T> addMsg(String msg) {
        this.msg = Strings.sNull(msg);
        return this;
    }

    public Result<T> addData(T data) {
        this.data = data;
        return this;
    }

    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, "操作成功");
    }

    public static <T> Result<T> success(String msg) {
        return new Result<>(SUCCESS_CODE, msg);
    }

    public static <T> Result<T> success(int code, String msg) {
        return new Result<>(code, null, msg);
    }

    public static <T> Result<T> success(T data) {
        return success(data, "操作成功");
    }

    public static <T> Result<T> success(T data, String msg) {
        return success(SUCCESS_CODE, data, msg);
    }

    public static <T> Result<T> success(int code, T data, String msg) {
        return new Result<>(code, data, data == null ? "操作成功" : msg);
    }

    public static <T> Result<T> data(T data) {
        return data(data, "操作成功");
    }

    public static <T> Result<T> data(T data, String msg) {
        return data(SUCCESS_CODE, data, msg);
    }

    public static <T> Result<T> data(int code, T data, String msg) {
        return new Result<>(code, data, data == null ? "数据不存在" : msg);
    }

    public static <T> Result<T> error() {
        return new Result<>(ERROR_CODE, "操作失败");
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(ERROR_CODE, msg);
    }

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, null, msg);
    }

    public static <T> Result<T> error(int code) {
        return new Result<>(code, "操作失败");
    }

    public static <T> Result<T> error(int code,T data) {
        return new Result<>(code, data,"操作失败");
    }
  
}

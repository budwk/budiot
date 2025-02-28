package com.budwk.app.access.thirdparty.aep.model;

import java.io.Serializable;

/**
 * 接口封装的返回结果
 */
public class ApiResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public ApiResult() {
    }

    public ApiResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }


    private int code;

    private String msg;

    private Object data;


    @Override
    public String toString() {
        return "ApiResult{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }

    public static ApiResult success(String msg, Object data) {
        return new ApiResult(0, msg, data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public ApiResult addData(Object data) {
        this.data = data;
        return this;
    }

    public ApiResult addMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public ApiResult addCode(int code) {
        this.code = code;
        return this;
    }
}

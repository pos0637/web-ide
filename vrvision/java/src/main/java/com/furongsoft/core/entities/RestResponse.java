package com.furongsoft.core.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/**
 * Restful响应内容
 *
 * @author Alex
 */
@Getter
@Setter
@NoArgsConstructor
public class RestResponse {
    /**
     * HTTP状态码
     */
    private int code;

    /**
     * 错误码
     */
    private int errno;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private Object data;

    /**
     * 新令牌
     */
    private String newToken;

    public RestResponse(HttpStatus status) {
        this(status.value(), null, null);
    }

    public RestResponse(HttpStatus status, String message, Object data) {
        this(status.value(), message, data);
    }

    public RestResponse(HttpStatus status, String message, Object data, String newToken) {
        this(status.value(), message, data, newToken);
    }

    public RestResponse(int code, String message, Object data) {
        this.code = code;
        this.errno = (HttpStatus.OK.value() == code) ? 0 : -1;
        this.message = message;
        this.data = data;
    }

    public RestResponse(int code, String message, Object data, String newToken) {
        this.code = code;
        this.errno = (HttpStatus.OK.value() == code) ? 0 : -1;
        this.message = message;
        this.data = data;
        this.newToken = newToken;
    }
}

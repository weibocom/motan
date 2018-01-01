package com.weibo.api.motan.demo.api.std;

import java.io.Serializable;

public class StdResponse<T> implements Serializable {

//    1 消息
//▪ 100 Continue
//▪ 101 Switching Protocols
//▪ 102 Processing
//2 成功
//▪ 200 OK
//▪ 201 Created
//▪ 202 Accepted
//▪ 203 Non-Authoritative Information
//▪ 204 No Content
//▪ 205 Reset Content
//▪ 206 Partial Content
//▪ 207 Multi-Status
//3 重定向
//▪ 300 Multiple Choices
//▪ 301 Moved Permanently
//▪ 302 Move temporarily
//▪ 303 See Other
//▪ 304 Not Modified
//▪ 305 Use Proxy
//▪ 306 Switch Proxy
//▪ 307 Temporary Redirect
//4 请求错误
//▪ 400 Bad Request
//▪ 401 Unauthorized
//▪ 402 Payment Required
//▪ 403 Forbidden
//▪ 404 Not Found
//▪ 405 Method Not Allowed
//▪ 406 Not Acceptable
//▪ 407 Proxy Authentication Required
//▪ 408 Request Timeout
//▪ 409 Conflict
//▪ 410 Gone
//▪ 411 Length Required
//▪ 412 Precondition Failed
//▪ 413 Request Entity Too Large
//▪ 414 Request-URI Too Long
//▪ 415 Unsupported Media Type
//▪ 416 Requested Range Not Satisfiable
//▪ 417 Expectation Failed
//▪ 421 too many connections
//▪ 422 Unprocessable Entity
//▪ 423 Locked
//▪ 424 Failed Dependency
//▪ 425 Unordered Collection
//▪ 426 Upgrade Required
//▪ 449 Retry With
//▪ 451Unavailable For Legal Reasons
//5 服务器错误（5、6字头）
//            ▪ 500 Internal Server Error
//▪ 501 Not Implemented
//▪ 502 Bad Gateway
//▪ 503 Service Unavailable
//▪ 504 Gateway Timeout
//▪ 505 HTTP Version Not Supported
//▪ 506 Variant Also Negotiates
//▪ 507 Insufficient Storage
//▪ 509 Bandwidth Limit Exceeded
//▪ 510 Not Extended
//▪ 600 Unparseable Response Headers

    public static final int CODE_OK = 200;
    public static final int CODE_CREATED = 201;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_INTERNAL_SERVER_ERROR = 500;

    public static final int CODE_DEFAULT_ERROR = CODE_OK;

    private final long timestamp;

    private final int code;

    /**
     * 显示给用户看的错误消息
     */
    private final String message;

    /**
     * 内部错误消息
     */
    private final String internalMessage;

    private final T data;

    public StdResponse(int code, String message, String internalMessage, T data) {
        this.timestamp = System.currentTimeMillis();
        this.code = code;
        this.message = message;
        this.internalMessage = internalMessage;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public boolean success() {
        return getCode() >= 200 && getCode() < 300;
    }

    public static <T> StdResponse<T> asSuccess(T result) {
        return new StdResponse<T>(CODE_OK, "操作成功", "操作成功", result);
    }

    public static StdResponse asSuccess() {
        return asSuccess(null);
    }

    public static <T> StdResponse<T> asError(String message, String internalMessage) {
        return new StdResponse<T>(CODE_DEFAULT_ERROR, message, internalMessage, null);
    }

    public static <T> StdResponse<T> asError(int code, String message, String internalMessage) {
        return new StdResponse<T>(code, message, internalMessage, null);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getInternalMessage() {
        return internalMessage;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ResultVo{" +
                "timestamp=" + timestamp +
                ", code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}

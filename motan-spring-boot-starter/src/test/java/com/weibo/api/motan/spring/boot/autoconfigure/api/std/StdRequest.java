package com.weibo.api.motan.spring.boot.autoconfigure.api.std;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@ToString
@AllArgsConstructor
public class StdRequest<T> implements Serializable {

    private final long timestamp;

    private String requestId;

    private final T data;

}

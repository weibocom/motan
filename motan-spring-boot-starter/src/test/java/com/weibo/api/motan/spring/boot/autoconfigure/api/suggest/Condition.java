package com.weibo.api.motan.spring.boot.autoconfigure.api.suggest;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class Condition implements Serializable {

    private String keyword;
    private String scope;
}

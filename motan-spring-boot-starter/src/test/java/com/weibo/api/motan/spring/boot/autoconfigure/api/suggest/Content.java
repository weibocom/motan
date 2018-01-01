package com.weibo.api.motan.spring.boot.autoconfigure.api.suggest;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class Content implements Serializable {

    private Long id;
    private String title;
    private String text;
    private Long count;
}

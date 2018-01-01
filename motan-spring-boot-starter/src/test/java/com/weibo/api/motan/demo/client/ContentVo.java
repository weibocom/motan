package com.weibo.api.motan.demo.client;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ContentVo {

    private Long id;
    private String title;
    private String text;
    private Long count;
}

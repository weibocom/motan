package com.weibo.api.motan.demo.client;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class SearchRes {

    private List<ContentVo> list;
}

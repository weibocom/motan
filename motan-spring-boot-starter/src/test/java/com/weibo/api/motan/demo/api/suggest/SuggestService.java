package com.weibo.api.motan.demo.api.suggest;

import com.weibo.api.motan.demo.api.std.StdResponse;

public interface SuggestService {

    StdResponse<ContentWrapper> query(String keyword);

}

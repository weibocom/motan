package com.weibo.api.motan.spring.boot.autoconfigure.api.suggest;

import com.weibo.api.motan.spring.boot.autoconfigure.api.std.StdResponse;

public interface SuggestService {

    StdResponse<ContentWrapper> query(String keyword);

}

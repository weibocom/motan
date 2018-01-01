package com.weibo.api.motan.spring.boot.autoconfigure.client;

import com.weibo.api.motan.spring.boot.autoconfigure.api.std.StdRequest;
import com.weibo.api.motan.spring.boot.autoconfigure.api.std.StdResponse;
import com.weibo.api.motan.spring.boot.autoconfigure.api.suggest.Condition;
import com.weibo.api.motan.spring.boot.autoconfigure.api.suggest.ContentWrapper;
import com.weibo.api.motan.spring.boot.autoconfigure.api.suggest.SuggestService;
import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import com.weibo.api.motan.spring.boot.autoconfigure.properties.MotanProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SuggestController {

    @MotanReferer(basicReferer = MotanProperties.DEFAULT_REFERER_CONFIG_NAME)
    private SuggestService suggestService;

    @GetMapping(value = "/search")
    public SearchRes search(String kw) {
        long timestamp = System.currentTimeMillis();
        Condition cond = new Condition();
        cond.setKeyword(kw);
        cond.setScope("all");
        StdRequest<Condition> req = new StdRequest<Condition>(timestamp, "", cond);
        StdResponse<ContentWrapper> response = suggestService.query(kw);
        ContentWrapper wrapper = response.getData();
        SearchRes searchRes = new SearchRes();
        if (wrapper != null) {
            BeanUtils.copyProperties(wrapper, searchRes);
        }
        return searchRes;
    }
}

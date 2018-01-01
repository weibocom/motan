package com.weibo.api.motan.spring.boot.autoconfigure.server;

import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import com.weibo.api.motan.spring.boot.autoconfigure.api.std.StdResponse;
import com.weibo.api.motan.spring.boot.autoconfigure.api.suggest.ContentWrapper;
import com.weibo.api.motan.spring.boot.autoconfigure.api.suggest.SuggestService;
import com.weibo.api.motan.spring.boot.autoconfigure.utils.JsonUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import java.net.URLEncoder;

@MotanService
@Setter
@Slf4j
public class SuggestServiceImpl implements SuggestService {

    @Override
    public StdResponse<ContentWrapper> query(String keyword) {
        if (keyword == null) {
            return StdResponse.asError(StdResponse.CODE_BAD_REQUEST, "参数不合法", "参数不合法: " + keyword);
        }

        log.info("收到rpc请求: keyword={}", keyword);

        try {
            String url = "http://www.ximalaya.com/search/suggest?scope=all&kw=" + URLEncoder.encode(keyword, "UTF-8");

            String json = Jsoup.connect(url).ignoreContentType(true).execute().body();
            ContentWrapper res = JsonUtils.toObject(json, ContentWrapper.class);

            return StdResponse.asSuccess(res);
        } catch (Exception e) {
            log.error("服务器内部错误: keyword={}", keyword, e);
            return StdResponse.asError(StdResponse.CODE_INTERNAL_SERVER_ERROR, "服务器内部错误", "服务器内部错误: " + e.getMessage());
        }
    }
}
